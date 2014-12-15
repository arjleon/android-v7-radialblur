#radial blur
===============

Simple project to apply a radial blur through a convolution matrix to Bitmap objects. This is aimed to support Android API level 7 because of the lack of RenderScript.

Since radial blur depends on an specific radius, a big image would slow down the process significantly. Therefore, as a small hack images are resized to smalled versions before running the algorithm. This allows for a faster processing of images.

**Several libraries offer RenderScript implementations already. Keep that in mind if you solely cover Android versions that support RenderScript for native performance**

##Usage

Still getting used to Android Studio's way of handling library projects/modules

Include in Android Studio project:

app.gradle:

```
compile project(':radialblur')
```

settings.gradle:

```
include ':radialblur'
project(':radialblur').projectDir = new File(settingsDir, '../RadialBlur/app')
```

```
RadialBlur radialBlur = RadialBlur.getInstance(3); //3 = blur radius, i.e. 3, 7, etc.
radialBlur.blurBitmap(originalBitmap, new RadialBlur.RadialBlurListener() {
	
	@Override
	public void onBitmapBlurred(Bitmap bitmap) {
		mBlurredImage.setImageBitmap(bitmap);
	}
});
```

####Future features:
* Include an id to all requests so parallel processing of several images can result in making use of the same callback/listener and handle each call based on that id.
* Allow different blur size/radius for each of the requests.
* Re-use cached kernels that were initialized with different blur sizes for an optimal convolution matrix.
