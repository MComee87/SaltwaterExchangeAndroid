package com.boonex.oo.media;

import java.io.File;
import java.io.IOException;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;

import com.boonex.oo.Connector;
import com.boonex.oo.Main;
import com.boonex.oo.R;

public class AddVideoActivity extends AddMediaActivity {
	private static final String TAG = "AddVideoActivity";	
	private static final String DEFAULT_EXTENSION = "3gp";
	private static final String DEFAULT_MIMETYPE = "video/3gpp";
	private static final String TMP_FILE = "tmp_video";
	
	protected Uri m_uriVideo;
	protected String m_sTmpFile = null;
    
	public AddVideoActivity() {
		super();
		sGalleryFilesType = "video/*";
	}
	
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b); 
                
        Object data = getLastNonConfigurationInstance();
        if (data != null)
        	m_uriVideo = (Uri)data;
        
        if (null != m_uriVideo)
        	previewVideo(m_uriVideo);
                
        m_buttonFromCamera.setOnClickListener(new View.OnClickListener(){            
            public void onClick(View view) {

        		Intent mIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);        			
        		mIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 300);
        		mIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // 0 - MMS quality, 1 - high quality

				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
					File file = null;
					try {
						file = File.createTempFile(TMP_FILE, ".vid", m_actAddMedia.getFilesDir());
					} catch (IOException e) {
						showToast(e.toString());
						return;
					}

					m_sTmpFile = file.getName();

					Uri fileURI = FileProvider.getUriForFile(m_actAddMedia,
							"com.example.android.fileprovider",
							file);

					mIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileURI);
				}

        		startActivityForResult(mIntent, CAMERA_ACTIVITY);
        		
            }
        });

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			m_buttonFromCamera.setEnabled(false);
			m_buttonFromGallery.setEnabled(false);
			ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
		}
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == 0) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				m_buttonFromCamera.setEnabled(true);
			}
			if (grantResults.length > 2 && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
				m_buttonFromGallery.setEnabled(true);
			}
		}
	}

    @Override
    public Object onRetainNonConfigurationInstance() {    	
        return m_uriVideo;
    }
        
    @Override
    protected void actionSubmitFile() {
        Connector o = Main.getConnector();                
        
        if (0 == m_editTitle.getText().length() || null == m_uriVideo) {
        	showErrorDialog(R.string.media_form_error, false);
        	return;
        }
                                                
        File file = new File(getRealPathFromURI(m_uriVideo));
        if (isFileTooBig(file.length(), true))
        	return;
        
        byte[] ba;
		try {
			ba = readFile(file);
		} catch (IOException e) {
			e.printStackTrace();
			showToast(e.toString());
			return;
        } catch (OutOfMemoryError e) {
        	Log.d(TAG, "Out of memory: " + e);
        	return;
        } 

        Object[] aParams = {
        		o.getUsername(), 
        		o.getPassword(),
        		m_sAlbumName,
        		ba,
        		Integer.valueOf(ba.length).toString(),
        		m_editTitle.getText().toString(),
        		m_editTags.getText().toString(),
        		m_editDesc.getText().toString(),
        		getExtFromURI(m_uriVideo)
        };                    
        
        o.execAsyncMethod("dolphin.uploadVideo", aParams, new Connector.Callback() {
			public void callFinished(Object result) {
				Log.d(TAG, "dolphin.uploadVideo result: " + result.toString());
				
				if (!result.toString().equals("ok")) {
					showErrorDialog(R.string.media_upload_failed, true);
				} else {
					showToast(R.string.media_upload_success_pending_conversion);
					Connector o = Main.getConnector();
					o.setImagesReloadRequired(true);
					o.setAlbumsReloadRequired(true);
					isFileTooBig (0.0, false); // TODO: remove
					finish();
				}
			}
        }, m_actAddMedia);

    }
    
	/**
	 * Retrieves the returned image from the Intent, inserts it into the MediaStore, which
	 *  automatically saves a thumbnail. Then assigns the thumbnail to the ImageView.
	 *  @param requestCode is the sub-activity code
	 *  @param resultCode specifies whether the activity was cancelled or not
	 *  @param intent is the data packet passed back from the sub-activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {		
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_CANCELED) {
			showToast(R.string.media_activity_canceled);
			return;
		}
				
		if (requestCode == CAMERA_ACTIVITY || requestCode == PICKER_ACTIVITY) {
			System.gc();
			
			m_uriVideo = intent.getData();

			Log.i(TAG, "Video URI: " + m_uriVideo); // file:///mnt/sdcard/tmp_video.3gp   |   content://media/external/video/media/5

			if (requestCode == CAMERA_ACTIVITY && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				File file = new File(m_actAddMedia.getFilesDir(), null == m_sTmpFile ? TMP_FILE + ".vid" : m_sTmpFile);
				if (!isFileTooBig(file.length(), true)) {
					m_uriVideo = Uri.fromFile(file);
					this.previewVideo(m_uriVideo);
					showToast(R.string.media_file_selected);
				} else {
					m_uriVideo = null;
				}
			}
			else if (null != getRealPathFromURI(m_uriVideo)) {
				Log.i(TAG, "Video Real Path: " + getRealPathFromURI(m_uriVideo));
				Log.i(TAG, "Video Type: " + getMimeTypeFromURI(m_uriVideo)); // video/mp4, video/3gpp
				if (!isFileTooBig((new File(getRealPathFromURI(m_uriVideo))).length(), true)) {
					this.previewVideo(m_uriVideo);
					showToast(R.string.media_file_selected);
				} else {
					m_uriVideo = null;
				}
			}
			else {
				showToast(R.string.media_error_from_camera_or_gallery);
				m_uriVideo = null;
			}
		}

	}

	protected void previewVideo (Uri videoUri) {
		Bitmap thumb = ThumbnailUtils.createVideoThumbnail(getRealPathFromURI(videoUri), MediaStore.Images.Thumbnails.MINI_KIND);
		if (null == thumb)
			return;
		m_viewFileThumb.setImageBitmap(thumb);
	}
	
	protected String getExtFromURI(Uri contentUri) {
		String sMimeType = getMimeTypeFromURI(contentUri);
		if (sMimeType.equals("video/mp4"))
			return "mp4";
		else // "video/3gpp"
			return DEFAULT_EXTENSION;
	}
	
	protected String getMimeTypeFromURI(Uri contentUri) {
		if (contentUri.toString().startsWith("file://")) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {			
				MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();	        	
	        	String sMimeType;
	        	try {
	        		metaRetriver.setDataSource(this, contentUri);
	        		sMimeType = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
	        	} catch (Exception e) {
	        		Log.d(TAG, "Getting MIMETYPE error: " + e.toString());
	        		sMimeType = DEFAULT_MIMETYPE;
	        	}
	        	return sMimeType;
			} else {
				return "video/3gpp";
			}
		}
		return getDataFromURI(MediaStore.Images.Media.MIME_TYPE, contentUri);
	}

    @Override
	protected void onDestroy() {
    	System.gc();
		super.onDestroy();
	}	
}
