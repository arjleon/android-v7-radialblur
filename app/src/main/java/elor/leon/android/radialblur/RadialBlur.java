package elor.leon.android.radialblur;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;

public class RadialBlur {

    private static RadialBlur mInstance;
    private static float[][] mKernel;
    private static int mBlurSize;
    private static int mResizeDimension;

    /**
     * By making use of the singleton pattern we can initialize the matrix to be used in
     * the blurring process in several Bitmap objects, saving time.
     */
    public static RadialBlur getInstance(int fixedBlurSize) {
        if (mInstance == null) {
            mInstance = new RadialBlur(fixedBlurSize);
        }

        return mInstance;
    }

    private RadialBlur(int fixedBlurSize) {
        initializeKernel(fixedBlurSize);
    }

    private void initializeKernel(int blurSize) {
        //even? make it the next odd
        if (mBlurSize % 2 == 0) {
            mBlurSize++;
        }

        //no need to reinitialize if the kernel is instantiated with the same blur size
        if (mKernel != null && mBlurSize == blurSize) {
            return;
        }

        mBlurSize = blurSize;
        mKernel = new float[mBlurSize][mBlurSize];

        //attempt to lower processing times if requested blur size is relatively high
        mResizeDimension = getResizeDimensionWithBlurSize(mBlurSize);

        //get radius of radial area in the kernel
        //then apply a ax priority to all points within blur radius
        //all points outside of radius are = 1 - (distanceToRadius - Radius)

        float radius, dx, dy, distance, value;
        radius = (float) Math.floor(mBlurSize / 2f);

        for (int x = 0; x < mBlurSize; x++) {
            for (int y = 0; y < mBlurSize; y++) {

                dx = Math.abs(x - radius);
                dy = Math.abs(y - radius);

                //distance from radius
                distance = (float) Math.sqrt(((double) dx * dx) + ((double) dy * dy));

                //set 1f if within radius, decrease value in radial manner based on distance
                value = distance <= radius ? 1f : 1 - (distance - (float)radius);

                mKernel[x][y] = value;
            }
        }
    }

    private static final int getResizeDimensionWithBlurSize(int blurSize) {
        if (blurSize >= 12) {
            return 50;
        } else if (blurSize >= 8) {
            return 60;
        } else if (blurSize >= 5) {
            return 75;
        }
        return 90;
    }

    /**
     * Implement setBlurSize(int blurSize) eventually to support parallel blurring of Bitmaps at
     * differently-initialized kernels.
     *
     * public void setBlurSize(int blurSize) {}
     */

    public final void blurBitmap(Bitmap bitmap, final RadialBlurListener l) {
        new GaussianBlurBitmapGeneratorAsync(mBlurSize, l).execute(bitmap);
    }

    private static final Bitmap gaussianBlurBitmap(Bitmap original, int blurSize) {

        if (original == null) {
            return null;
        }

        //if it would not change image, return original
        if (blurSize <= 1) {
            return original;
        }

        original = resizeBitmap(original, mResizeDimension, false);

        final int h = original.getHeight();
        final int w = original.getWidth();
        Bitmap blurred = Bitmap.createBitmap(w, h, original.getConfig());

        int pixel;								//single pixel to analyze
        int subpixel;							//global subpixel inside local kernel
        int a, r, g, b;							//color channels of pixel
        int rTotal, gTotal, bTotal;				//average of channels of each subpixel
        int gx, gy;								//local kernel pixels turned global
        int localToGlobal = (blurSize - 1) / 2;//static result that will return global coords of local pixel in kernel when subtracted
        int kernelCount;						//number of processed sorrounding pixels

        //cycle every pixel (w and h of original)
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                //base alpha value and starting color channel totals
                pixel = original.getPixel(x, y);
                a = Color.alpha(pixel);
                rTotal = gTotal = bTotal = 0;
                kernelCount = 0;

                //cycle through all kernel pixels while centered to global pixel
                for (int kx = 0; kx < blurSize; kx++) {
                    for (int ky = 0; ky < blurSize; ky++) {
                        gx = x + kx - localToGlobal;
                        gy = y + ky - localToGlobal;

                        //once local kernel coords are global to whole image
                        // make sure it's within boundaries and apply weight
                        // to obtaining channel value
                        if (gx >= 0 && gx < w && gy >= 0 && gy < h) {
                            subpixel = original.getPixel(gx, gy);
                            rTotal += ((float) Color.red(subpixel)) * ((float) mKernel[kx][ky]);
                            gTotal += (float) Color.green(subpixel) * mKernel[kx][ky];
                            bTotal += (float) Color.blue(subpixel) * mKernel[kx][ky];
                            kernelCount++;
                        }
                    }
                }

                r = rTotal/kernelCount;
                g = gTotal/kernelCount;
                b = bTotal/kernelCount;

                blurred.setPixel(x, y, Color.argb(a,r, g, b));
            }
        }

        return blurred;
    }

    private static Bitmap resizeBitmap(Bitmap bitmap, int maximumDimension, boolean scaleUpIfSmaller) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float newScale;

        if (Math.max(width, height) <= maximumDimension && !scaleUpIfSmaller) {
            return bitmap;
        }

        if (width > height) {
            newScale = (float)maximumDimension / (float)width;
        } else {
            newScale = (float)maximumDimension / (float)height;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(newScale, newScale);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private static class GaussianBlurBitmapGeneratorAsync extends AsyncTask<Bitmap, Void, Bitmap> {

        private RadialBlurListener mListener;
        private int mRadius;

        public GaussianBlurBitmapGeneratorAsync(int radius, final RadialBlurListener l) {
            mRadius = radius;
            mListener = l;
        }

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            return gaussianBlurBitmap(params[0], mRadius);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mListener != null) {
                mListener.onBitmapBlurred(result);
            }
        }
    }

    public interface RadialBlurListener {
        void onBitmapBlurred(Bitmap bitmap);
    }
}
