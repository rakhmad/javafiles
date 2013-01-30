package com.msci.moslem.util;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This class provides function for resizing images. We use Thumbnailator
 * as library.
 *
 * @author Rakhmad Azhari <r.azhari@samsung.com>
 * @version 0.0.1
 */
public class ImageHelper {

    private static ImageHelper instance;
    private String input, output;

    private ImageHelper() {
    }

    public static ImageHelper getInstance() {
        if (instance == null) {
            return new ImageHelper();
        }
        return instance;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void resizeWithScale() throws IOException {
        BufferedImage sourceImage = ImageIO.read(new File(input));
        Thumbnails.of(sourceImage).scale(0.5f).outputFormat("png").toFile(output);
    }
}
