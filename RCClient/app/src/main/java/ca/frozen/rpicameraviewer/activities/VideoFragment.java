// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.holgis.rcclient.R;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ca.frozen.library.classes.Log;
import ca.frozen.library.views.ZoomPanTextureView;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.HttpReader;
import ca.frozen.rpicameraviewer.classes.MulticastReader;
import ca.frozen.rpicameraviewer.classes.RawH264Reader;
import ca.frozen.rpicameraviewer.classes.Source;
import ca.frozen.rpicameraviewer.classes.SpsParser;
import ca.frozen.rpicameraviewer.classes.TcpIpReader;

public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener
{
	// public constants
	public final static String CAMERA = "camera";
	public final static String FULL_SCREEN = "full_screen";

	// local constants
	private final static float MIN_ZOOM = 0.1f;
	private final static float MAX_ZOOM = 10;

	// instance variables
	private Camera camera;
	private boolean fullScreen;
	private DecoderThread decoder;
	private ZoomPanTextureView textureView;
	private Runnable startVideoRunner;
	private Handler startVideoHandler;

	//******************************************************************************
	// newInstance
	//******************************************************************************
	public static VideoFragment newInstance(Camera camera, boolean fullScreen)
	{
		VideoFragment fragment = new VideoFragment();

		Bundle args = new Bundle();
		args.putParcelable(CAMERA, camera);
		args.putBoolean(FULL_SCREEN, fullScreen);
		fragment.setArguments(args);

		return fragment;
	}

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
        setRetainInstance(true);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		String server = prefs.getString("PREF_IP_ADDR", "192.168.0.140");

        camera = new Camera(Source.ConnectionType.RawTcpIp, "", server, 5001);

		// get the parameters
		fullScreen = false;//getArguments().getBoolean(FULL_SCREEN);
		Log.info("camera: " + camera.toString());

		// create the start video handler and runnable
		startVideoHandler = new Handler();
		startVideoRunner = new Runnable()
		{
			@Override
			public void run()
			{
				MediaFormat format = decoder.getMediaFormat();
				int videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
				int videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
				textureView.setVideoSize(videoWidth, videoHeight);
			}
		};
	}

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.video_fragment, container, false);

		// set the texture listener
		textureView = (ZoomPanTextureView)view.findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);
		textureView.setZoomRange(MIN_ZOOM, MAX_ZOOM);

		return view;
	}

	//******************************************************************************
	// onAttach
	//******************************************************************************
	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		try
		{
			Activity activity = (Activity) context;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(context.toString() + " must implement OnFadeListener");
		}
	}

	//******************************************************************************
	// onDestroy
	//******************************************************************************
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	//******************************************************************************
	// onStart
	//******************************************************************************
	@Override
	public void onStart()
	{
		super.onStart();

		// create the decoder thread
		decoder = new DecoderThread();
		decoder.start();
	}

	//******************************************************************************
	// onStop
	//******************************************************************************
	@Override
	public void onStop()
	{
		super.onStop();

		if (decoder != null)
		{
			decoder.interrupt();
			decoder = null;
		}
	}

	//******************************************************************************
	// onPause
	//******************************************************************************
	@Override
	public void onPause()
	{
		super.onPause();
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		super.onResume();

	}

	//******************************************************************************
	// onSurfaceTextureAvailable
	//******************************************************************************
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
	{
		if (decoder != null)
		{
			decoder.setSurface(new Surface(surfaceTexture), startVideoHandler, startVideoRunner);
		}
	}

	//******************************************************************************
	// onSurfaceTextureSizeChanged
	//******************************************************************************
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height)
	{
	}

	//******************************************************************************
	// onSurfaceTextureDestroyed
	//******************************************************************************
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
	{
		if (decoder != null)
		{
			decoder.setSurface(null, null, null);
		}
		return true;
	}

	//******************************************************************************
	// onSurfaceTextureUpdated
	//******************************************************************************
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
	{
	}


	//******************************************************************************
	// stop
	//******************************************************************************
	public void stop()
	{
		if (decoder != null)
		{
			decoder.interrupt();
			try
			{
				decoder.join(TcpIpReader.IO_TIMEOUT * 2);
			}
			catch (Exception ex) {}
			decoder = null;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// DecoderThread
	////////////////////////////////////////////////////////////////////////////////
	private class DecoderThread extends Thread
	{
		// local constants
		private final static int MULTICAST_BUFFER_SIZE = 16384;
		private final static int TCPIP_BUFFER_SIZE = 16384;
		private final static int HTTP_BUFFER_SIZE = 4096;
		private final static int NAL_SIZE_INC = 4096;
		private final static int MAX_READ_ERRORS = 300;

		// instance variables
		private MediaCodec decoder = null;
		private MediaFormat format;
		private boolean decoding = false;
		private Surface surface;
		private Source source = null;
		private byte[] buffer = null;
		private ByteBuffer[] inputBuffers = null;
		private long presentationTime;
		private long presentationTimeInc = 66666;
		private RawH264Reader reader = null;
		private WifiManager.MulticastLock multicastLock = null;
		private Handler startVideoHandler;
		private Runnable startVideoRunner;

		//******************************************************************************
		// setSurface
		//******************************************************************************
		public void setSurface(Surface surface, Handler handler, Runnable runner)
		{
			this.surface = surface;
			this.startVideoHandler = handler;
			this.startVideoRunner = runner;
			if (decoder != null)
			{
				if (surface != null)
				{
					boolean newDecoding = decoding;
					if (decoding)
					{
						setDecodingState(false);
					}
					if (format != null)
					{
						try
						{
							decoder.configure(format, surface, null, 0);
						}
						catch (Exception ex) {}
						if (!newDecoding)
						{
							newDecoding = true;
						}
					}
					if (newDecoding)
					{
						setDecodingState(newDecoding);
					}
				}
				else if (decoding)
				{
					setDecodingState(false);
				}
			}
		}

		//******************************************************************************
		// getMediaFormat
		//******************************************************************************
		public MediaFormat getMediaFormat()
		{
			return format;
		}

		//******************************************************************************
		// setDecodingState
		//******************************************************************************
		private synchronized void setDecodingState(boolean newDecoding)
		{
			try
			{
				if (newDecoding != decoding && decoder != null)
				{
					if (newDecoding)
					{
						decoder.start();
					}
					else
					{
						decoder.stop();
					}
					decoding = newDecoding;
				}
			} catch (Exception ex) {}
		}

		//******************************************************************************
		// run
		//******************************************************************************
		@Override
		public void run()
		{
			byte[] nal = new byte[NAL_SIZE_INC];
			int nalLen = 0;
			int numZeroes = 0;
			int numReadErrors = 0;

			try
			{
				// get the multicast lock if necessary
				if (camera.source.connectionType == Source.ConnectionType.RawMulticast)
				{
					WifiManager wifi = (WifiManager)getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
					if (wifi != null)
					{
						multicastLock = wifi.createMulticastLock("rpicamlock");
						multicastLock.acquire();
					}
				}

				// create the decoder
				decoder = MediaCodec.createDecoderByType("video/avc");

				// create the reader
				source = camera.getCombinedSource();
				if (source.connectionType == Source.ConnectionType.RawMulticast)
				{
					buffer = new byte[MULTICAST_BUFFER_SIZE];
					reader = new MulticastReader(source);
				}
				else if (source.connectionType == Source.ConnectionType.RawHttp)
				{
					buffer = new byte[HTTP_BUFFER_SIZE];
					reader = new HttpReader(source);
				}
				else
				{
					buffer = new byte[TCPIP_BUFFER_SIZE];
					reader = new TcpIpReader(source);
				}
				if (!reader.isConnected())
				{
					throw new Exception();
				}

				// read from the source
				while (!isInterrupted())
				{
					// read from the stream
					int len = reader.read(buffer);
					if (isInterrupted()) break;

					// process the input buffer
					if (len > 0)
					{
						numReadErrors = 0;
						for (int i = 0; i < len && !isInterrupted(); i++)
						{
							// add the byte to the NAL
							if (nalLen == nal.length)
							{
								nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);
							}
							nal[nalLen++] = buffer[i];

							// look for a header
							if (buffer[i] == 0)
							{
								numZeroes++;
							}
							else
							{
								if (buffer[i] == 1 && numZeroes == 3)
								{
									if (nalLen > 4)
									{
										int nalType = processNal(nal, nalLen - 4);
										if (isInterrupted()) break;
										if (nalType == -1)
										{
											nal[0] = nal[1] = nal[2] = 0;
											nal[3] = 1;
										}
									}
									nalLen = 4;
								}
								numZeroes = 0;
							}
						}
					}
					else
					{
						numReadErrors++;
						if (numReadErrors >= MAX_READ_ERRORS)
						{
                            Log.debug("Connection lost");
							break;
						}
					}

					// send an output buffer to the surface
					if (format != null && decoding)
					{
						if (isInterrupted()) break;
						MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
						int index;
						do
						{
							index = decoder.dequeueOutputBuffer(info, presentationTimeInc);
							if (isInterrupted()) break;
							if (index >= 0)
							{
								decoder.releaseOutputBuffer(index, true);
							}
							//Log.info(String.format("dequeueOutputBuffer index = %d", index));
						} while (index >= 0);
					}
				}
			}
			catch (Exception ex)
			{
				Log.error(ex.toString());
				if (reader == null || !reader.isConnected())
				{
                    Log.debug("Could not connect");
				}
				else
				{
                    Log.debug("Connection lost");
				}
				ex.printStackTrace();
			}

			// close the reader
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (Exception ex) {}
				reader = null;
			}

			// stop the decoder
			if (decoder != null)
			{
				try
				{
					setDecodingState(false);
					decoder.release();
				}
				catch (Exception ex) {}
				decoder = null;
			}

			// release the multicast lock
			if (multicastLock != null)
			{
				try
				{
					if (multicastLock.isHeld())
					{
						multicastLock.release();
					}
				}
				catch (Exception ex) {}
				multicastLock = null;
			}
		}

		//******************************************************************************
		// processNal
		//******************************************************************************
		private int processNal(byte[] nal, int nalLen)
		{
			// get the NAL type
			int nalType = (nalLen > 4 && nal[0] == 0 && nal[1] == 0 && nal[2] == 0 && nal[3] == 1) ? (nal[4] & 0x1F) : -1;
			//Log.info(String.format("NAL: type = %d, len = %d", nalType, nalLen));

			// process the first SPS record we encounter
			if (nalType == 7 && !decoding)
			{
				SpsParser parser = new SpsParser(nal, nalLen);
				int width = (source.width != 0) ? source.width : parser.width;
				int height = (source.height != 0) ? source.height : parser.height;
				format = MediaFormat.createVideoFormat("video/avc", width, height);
				if (source.fps != 0)
				{
					format.setInteger(MediaFormat.KEY_FRAME_RATE, source.fps);
					presentationTimeInc = 1000000 / source.fps;
				}
				else
				{
					presentationTimeInc = 66666;
				}
				presentationTime = System.nanoTime() / 1000;
				if (source.bps != 0)
				{
					format.setInteger(MediaFormat.KEY_BIT_RATE, source.bps);
				}
				Log.info(String.format("SPS: %02X, %d x %d, %d, %d, %d", nal[4], width, height, source.fps, source.bps, presentationTimeInc));
				decoder.configure(format, surface, null, 0);
				setDecodingState(true);
				inputBuffers = decoder.getInputBuffers();
				startVideoHandler.post(startVideoRunner);
			}

			// queue the frame
			if (nalType > 0 && decoding)
			{
				int index = decoder.dequeueInputBuffer(presentationTimeInc);
				if (index >= 0)
				{
					ByteBuffer inputBuffer = inputBuffers[index];
					//ByteBuffer inputBuffer = decoder.getInputBuffer(index);
					inputBuffer.put(nal, 0, nalLen);
					decoder.queueInputBuffer(index, 0, nalLen, presentationTime, 0);
					presentationTime += presentationTimeInc;
				}
				//Log.info(String.format("dequeueInputBuffer index = %d", index));
			}
			return nalType;
		}


	}
}
