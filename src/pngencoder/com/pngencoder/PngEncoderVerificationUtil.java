package com.pngencoder;

import java.util.zip.Deflater;

class PngEncoderVerificationUtil {
    private PngEncoderVerificationUtil() {
    }

    static int verifyCompressionLevel(int compressionLevel) {
        if ((compressionLevel < -1) || (compressionLevel > 9)) {
            String message = String.format("The compressionLevel must be between -1 and 9 inclusive, but was %d.", compressionLevel);
            throw new IllegalArgumentException(message);
        }
        return compressionLevel;
    }
    
    static int verifyCompressionStrategy(int compressionStrategy) {
      if ((compressionStrategy == Deflater.DEFAULT_STRATEGY || (compressionStrategy == Deflater.FILTERED) || (compressionStrategy == Deflater.HUFFMAN_ONLY))) {
        return compressionStrategy;
      }
      String message = String.format("The compressionStrategy must be DEFAULT_STRATEGY or FILTERED or HUFFMAN_ONLY");
      throw new IllegalArgumentException(message);

  }

    static String verifyChunkType(String chunkType) {
        if (chunkType.length() != 4) {
            String message = String.format("The chunkType must be four letters, but was \"%s\". See http://www.libpng.org/pub/png/book/chapter08.html#png.ch08.div.1", chunkType);
            throw new IllegalArgumentException(message);
        }
        return chunkType;
    }
}
