package com.pngencoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Main class, containing the interface for PngEncoder.
 * <p>
 * PngEncoder is a really fast encoder for PNG images in Java.
 */
public class PngEncoder {
    public static final int COLOR_INDEXED = 0;
    public static final int INDEXED_COLORS_ORIGINAL = 0;
    public static final int COLOR_TRUECOLOR = 1;
    /**
     * Compression level 9 is the default.
     * <p>
     * It produces images with a size comparable to ImageIO.
     */
    public static int DEFAULT_COMPRESSION_LEVEL = Deflater.BEST_SPEED;
    public static int DEFAULT_COMPRESSION_STRATEGY = Deflater.FILTERED;

    private BufferedImage bufferedImage;
    private final int compressionLevel;
    private final int compressionStrategy;
    private final boolean multiThreadedCompressionEnabled;
    private final PngEncoderSrgbRenderingIntent srgbRenderingIntent;
    private final PngEncoderPhysicalPixelDimensions physicalPixelDimensions;
    private final Deflater deflater;

    private final boolean usePredictorEncoding;
    private final boolean tryIndexedEncoding;

    private PngEncoder(BufferedImage bufferedImage, int compressionLevel, int compressionStrategy, boolean multiThreadedCompressionEnabled,
            PngEncoderSrgbRenderingIntent srgbRenderingIntent,
            PngEncoderPhysicalPixelDimensions physicalPixelDimensions, boolean usePredictorEncoding, boolean tryIndexedEncoding) {
        this.bufferedImage = bufferedImage;
        this.compressionLevel = PngEncoderVerificationUtil.verifyCompressionLevel(compressionLevel);
        this.compressionStrategy = PngEncoderVerificationUtil.verifyCompressionStrategy(compressionStrategy);
        this.multiThreadedCompressionEnabled = multiThreadedCompressionEnabled;
        this.srgbRenderingIntent = srgbRenderingIntent;
        this.physicalPixelDimensions = physicalPixelDimensions;
        this.usePredictorEncoding = usePredictorEncoding;
        this.tryIndexedEncoding = tryIndexedEncoding;
        this.deflater = new Deflater();
    }

    /**
     * Constructs an empty PngEncoder. Usually combined with methods named with*.
     */
    public PngEncoder() {
        this(null, DEFAULT_COMPRESSION_LEVEL, DEFAULT_COMPRESSION_STRATEGY, false, null, null, false, false);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code bufferedImage}.
     * The new PngEncoder will use the provided {@code bufferedImage}.
     *
     * @param bufferedImage input image
     * @return a new PngEncoder
     */
    public PngEncoder withBufferedImage(BufferedImage bufferedImage) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code compressionLevel}.
     * The new PngEncoder will use the provided {@code compressionLevel}.
     *
     * @param compressionLevel input image (must be between -1 and 9 inclusive)
     * @return a new PngEncoder
     */
    public PngEncoder withCompressionLevel(int compressionLevel) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }
    
    public PngEncoder withCompressionStrategy(int compressionStrategy) {
      return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
              physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }

    /**
     * Try to encode the image with indexed encoding. This only works if the image is RGB and uses not more than
     * 256 colors.
     *
     * @param tryIndexedEncoding true if indexed encoding should be tried.
     * @return a new PngEncoder
     */
    public PngEncoder withTryIndexedEncoding(boolean tryIndexedEncoding) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code multiThreadedCompressionEnabled}.
     * The new PngEncoder will use the provided {@code multiThreadedCompressionEnabled}.
     *
     * @param multiThreadedCompressionEnabled when {@code true}, multithreaded compression will be used
     * @return a new PngEncoder
     */
    public PngEncoder withMultiThreadedCompressionEnabled(boolean multiThreadedCompressionEnabled) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code srgbRenderingIntent}.
     * The new PngEncoder will add an sRGB chunk to the encoded PNG and use the provided {@code srgbRenderingIntent}.
     *
     * @param srgbRenderingIntent the rendering intent that should be used when displaying the image
     * @return a new PngEncoder
     */
    public PngEncoder withSrgbRenderingIntent(PngEncoderSrgbRenderingIntent srgbRenderingIntent) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }

    public PngEncoder withPhysicalPixelDimensions(PngEncoderPhysicalPixelDimensions physicalPixelDimensions) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions, usePredictorEncoding, tryIndexedEncoding);
    }

    /**
     * Returns a new PngEncoder which has the same configuration as this one except {@code usePredictorEncoding}.
     * The new PngEncoder will use the provided {@code usePredictorEncoding}.
     *
     * @param usePredictorEncoding true if predictor encoding should be used.
     * @return a new PngEncoder
     */
    public PngEncoder withPredictorEncoding(boolean usePredictorEncoding) {
        return new PngEncoder(bufferedImage, compressionLevel, compressionStrategy, multiThreadedCompressionEnabled, srgbRenderingIntent,
                physicalPixelDimensions,
                usePredictorEncoding, tryIndexedEncoding);
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    /**
     * @return true if the predictor encoding is enabled.
     */
    public boolean isPredictorEncodingEnabled() {
        return usePredictorEncoding;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public boolean isMultiThreadedCompressionEnabled() {
        return multiThreadedCompressionEnabled;
    }

    public PngEncoderSrgbRenderingIntent getSrgbRenderingIntent() {
        return srgbRenderingIntent;
    }

    /**
     * Encodes the image to outputStream.
     *
     * @param outputStream destination of the encoded data
     * @return number of bytes written
     * @throws NullPointerException if the image has not been set.
     */
    public int toStream(OutputStream outputStream) {
        try {
            return PngEncoderLogic.encode(bufferedImage, outputStream, compressionLevel, compressionStrategy,
                    multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions,
                    isPredictorEncodingEnabled(), tryIndexedEncoding, deflater);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the image and saves data into {@code filePath}.
     *
     * @param filePath destination file where the encoded data will be written
     * @return number of bytes written
     * @throws NullPointerException if the image has not been set.
     * @throws UncheckedIOException instead of IOException
     */
    public int toFile(String filePath) {
        try {
          OutputStream outputStream = new FileOutputStream(filePath);
            return toStream(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the image and saves data into {@code file}.
     *
     * @param file destination file where the encoded data will be written
     * @return number of bytes written
     * @throws NullPointerException if the image has not been set.
     * @throws UncheckedIOException instead of IOException
     */
    public int toFile(File file) {
        return toFile(file.getAbsolutePath());
    }

    /**
     * Encodes the image and returns data as {@code byte[]}.
     *
     * @return encoded data
     * @throws NullPointerException if the image has not been set.
     */
    public byte[] toBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64 * 1024);
        toStream(outputStream);
        return outputStream.toByteArray();
    }
    
    public int encode(BufferedImage bufferedImage, OutputStream outputStream)
    {
      try {
        return PngEncoderLogic.encode(bufferedImage, outputStream, compressionLevel, compressionStrategy,
                multiThreadedCompressionEnabled, srgbRenderingIntent, physicalPixelDimensions,
                usePredictorEncoding, tryIndexedEncoding, deflater);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void setColorType(int colorType)
    {
      if (colorType == COLOR_INDEXED)
      {
        //usePredictorEncoding = true;
      }
      else
      {
        //usePredictorEncoding = false;
      }
    }

    public void setIndexedColorMode(int indexedColorMode)
    {
      
    }
 }
