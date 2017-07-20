/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.textureview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.util.Size;
import android.widget.FrameLayout;

import junit.framework.Assert;

public class ViewActivity extends Activity
		implements TextureView.SurfaceTextureListener,
		ActivityCompat.OnRequestPermissionsResultCallback {
	private  TextureView textureView_;
	Surface  surface_ = null;
	private Size cameraPreviewSize_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onWindowFocusChanged(true);
		setContentView(R.layout.activity_main);
		RequestCamera();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus)
		{
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	private void createTextureView() {
		textureView_ = (TextureView)findViewById(R.id.texturePreview);
		textureView_.setSurfaceTextureListener(this);
	}

	public void onSurfaceTextureAvailable(SurfaceTexture surface,
										  int width, int height) {
		createNativeCamera();

		resizeTextureView(width, height);
		surface.setDefaultBufferSize(cameraPreviewSize_.getWidth(),
				                     cameraPreviewSize_.getHeight());
        surface_ = new Surface(surface);
		onPreviewSurfaceCreated(surface_);
	}

	private void resizeTextureView(int textureWidth, int textureHeight)
	{
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int newWidth = textureWidth;
		int newHeight = textureWidth * cameraPreviewSize_.getWidth() / cameraPreviewSize_.getHeight();

		if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			newHeight = (textureWidth * cameraPreviewSize_.getHeight()) / cameraPreviewSize_.getWidth();
		}
		textureView_.setLayoutParams(
					new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER));
		configureTransform(newWidth, newHeight);
	}

	/**
	 * configureTransform()
	 *     Courtesy to https://github.com/google/cameraview/blob/master/library/src/main/api14/com/google/android/cameraview/TextureViewPreview.java#L108
	 * @param width TextureView width
	 * @param height is TextureView height
	 */
	void configureTransform(int width, int height) {
		int mDisplayOrientation = getWindowManager().getDefaultDisplay().getRotation() * 90;
		Matrix matrix = new Matrix();
		if (mDisplayOrientation % 180 == 90) {
			//final int width = getWidth();
			//final int height = getHeight();
			// Rotate the camera preview when the screen is landscape.
			matrix.setPolyToPoly(
					new float[]{
							0.f, 0.f, // top left
							width, 0.f, // top right
							0.f, height, // bottom left
							width, height, // bottom right
					}, 0,
					mDisplayOrientation == 90 ?
							// Clockwise
							new float[]{
									0.f, height, // top left
									0.f, 0.f,    // top right
									width, height, // bottom left
									width, 0.f, // bottom right
							} : // mDisplayOrientation == 270
							// Counter-clockwise
							new float[]{
									width, 0.f, // top left
									width, height, // top right
									0.f, 0.f, // bottom left
									0.f, height, // bottom right
							}, 0,
					4);
		} else if (mDisplayOrientation == 180) {
			matrix.postRotate(180, width / 2, height/2);
		}
		textureView_.setTransform(matrix);
	}

	public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
											int width, int height) {}

	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		onPreviewSurfaceDestroyed(surface_);
		// deleteCamera();

		surface_ = null;
		return true;
	}

	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	private static final int PERMISSION_REQUEST_CODE_CAMERA = 1;
	public void RequestCamera() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(
					this,
					new String[] { Manifest.permission.CAMERA },
					PERMISSION_REQUEST_CODE_CAMERA);
			return;
		}
		createTextureView();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
        /*
         * if any permission failed, the sample could not play
         */
		if (PERMISSION_REQUEST_CODE_CAMERA != requestCode) {
			super.onRequestPermissionsResult(requestCode,
					permissions,
					grantResults);
			return;
		}

		Assert.assertEquals(grantResults.length, 1);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			createTextureView();
		}
	}

	private void createNativeCamera() {
		int rotation = 90 * getWindowManager().getDefaultDisplay()
					        .getRotation();
		Display display = getWindowManager().getDefaultDisplay();
		int height = display.getMode().getPhysicalHeight();
		int width = display.getMode().getPhysicalWidth();

		long nativeCamera = createCamera(width, height, rotation);

		cameraPreviewSize_ = getMinimumCompatiblePreviewSize(nativeCamera);

	}

	/*
	 * Functions calling into NDKCamera side to:
	 *     CreateCamera / DeleteCamera object
	 *     Start/Stop Preview
	 *     Pulling Camera Parameters
	 */
	private native long createCamera(int width, int height, int rotation);
	private native void onPreviewSurfaceCreated(Surface surface);
	private native Size getMinimumCompatiblePreviewSize(long nativeObj);
	private native void onPreviewSurfaceDestroyed(Surface surface);

	static {
		System.loadLibrary("camera_textureview");
	}

}
