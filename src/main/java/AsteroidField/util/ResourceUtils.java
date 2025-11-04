package AsteroidField.util;

import AsteroidField.audio.AudioResourceProvider;
import AsteroidField.events.ApplicationEvent;
import AsteroidField.icons.IconResourceProvider;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Sean Phillips
 */
public enum ResourceUtils {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtils.class);

//    public static File saveImageFile(Image image) throws IOException {
//
//        File newFile = new File("imagery/Trinity-scan-" + UUID.randomUUID() + ".png");
//        BufferedImage buff = SwingFXUtils.fromFXImage(image, null);
//        ImageIO.write(buff, "PNG", newFile);
//        return newFile;
//    }

    public static WritableImage loadImageFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        WritableImage wi = SwingFXUtils.toFXImage(image, null);
        return wi;
    }

    public static WritableImage loadImageFile(String filename) throws IOException {
        File imageFile = new File(filename);
        return loadImageFile(imageFile);
    }

//    public static Image load3DTextureImage(String filename) throws IOException {
//        try {
//            return new Image(ImageResourceProvider.getResourceAsStream(filename + ".png"));
//        } catch (NullPointerException e) {
//            throw new IOException("Failed to open " + filename + ".png");
//        }
//    }

    public static WritableImage loadImageFileSubset(String filename,
                                                    int x1, int y1, int x2, int y2) throws IOException {
        File imageFile = new File(filename);
        BufferedImage image = ImageIO.read(imageFile);
        return loadImageSubset(image, x1, y1, x2, y2);
    }

    public static WritableImage loadImageSubset(BufferedImage image, int x1, int y1, int x2, int y2) {
        BufferedImage subImage = image.getSubimage(x1, y1, x2 - x1, y2 - y1);
        WritableImage wi = SwingFXUtils.toFXImage(subImage, null);
        return wi;
    }

    public static WritableImage cropImage(Image image, double x1, double y1, double x2, double y2) {
        PixelReader r = image.getPixelReader();
        WritablePixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
        int x1Index = Double.valueOf(x1).intValue();
        int y1Index = Double.valueOf(y1).intValue();
        int x2Index = Double.valueOf(x2).intValue();
        int y2Index = Double.valueOf(y2).intValue();
        int width = x2Index - x1Index;
        int height = y2Index - y1Index;
        int[] pixels = new int[width * height];
        r.getPixels(x1Index, y1Index, width, height, pixelFormat, pixels, 0, width);
        WritableImage out = new WritableImage(width, height);
        PixelWriter w = out.getPixelWriter();
        w.setPixels(0, 0, width, height, pixelFormat, pixels, 0, width);
        return out;
    }

    public static WritableImage loadIconAsWritableImage(String iconName) throws IOException {
        InputStream is = IconResourceProvider.getResourceAsStream(iconName + ".png");
        BufferedImage image = ImageIO.read(is);
        WritableImage wi = SwingFXUtils.toFXImage(image, null);
        return wi;
    }

    public static Image loadIconFile(String iconName) {
        try {
            return new Image(IconResourceProvider.getResourceAsStream(iconName + ".png"));
        } catch (NullPointerException e) {
            return new Image(IconResourceProvider.getResourceAsStream("noimage.png"));
        }
    }

    public static ImageView loadIcon(String iconName, double FIT_WIDTH) {
        ImageView iv = new ImageView(loadIconFile(iconName));
        iv.setPreserveRatio(true);
        iv.setFitWidth(FIT_WIDTH);
        return iv;
    }

    /**
     * Checks whether the file can be used as audio.
     *
     * @param file The File object to check.
     * @return boolean true if it is a file, can be read and is a supported audio type
     */
    public static boolean isAudioFile(File file) {
        if (file.isFile() && file.canRead()) {
            try {
                String contentType = Files.probeContentType(file.toPath());
                switch (contentType) {
                    case "audio/x-flac":
                    case "audio/flac":
                    case "audio/wav":
                    case "audio/mp3":
                        return true;
                }
                //System.out.println(contentType);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        }
        return false;
    }

    public static AudioClip loadAudioClipWav(String filename) {
        return new AudioClip(AudioResourceProvider.getResource(filename + ".wav").toExternalForm());
    }

    public static Media loadMediaWav(String filename) throws IOException {
        return new Media(AudioResourceProvider.getResource(filename + ".wav").toExternalForm());
    }

    public static Media loadMediaMp4(String filename) throws MalformedURLException {
        File folder = new File("video/");
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            return null;
        }
        for (File file : folder.listFiles()) {
            if (file.getName().contentEquals(filename)) {
                Media media = new Media(file.toURI().toURL().toString());
                return media;
            }
        }

        return null;
    }

    public static boolean canDragOverDirectory(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            File file = db.getFiles().get(0); //only support the first
            if (file.isDirectory()) {
                event.acceptTransferModes(TransferMode.COPY);
                return true;
            }
        } else {
            event.consume();
        }
        return false;
    }

    public static boolean canDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        DataFormat dataFormat = DataFormat.lookupMimeType("application/x-java-file-list");
        try {
            if (db.hasFiles() || db.hasContent(dataFormat)) {
                List<File> files = db.getFiles();
                //workaround for Swing JFXPanel
                if (db.hasContent(dataFormat)) {
                    //Swing containers require a registered mime type
                    //since we don't have that, we need to accept the drag
                    event.acceptTransferModes(TransferMode.COPY);
                    return true;
                }
            } else {
                event.consume();
            }
        } catch (Exception ex) {
            LOG.error(null, ex);
            event.consume();
        }
        return false;
    }

    /**
     * Any time a drop event occurs this attempts to process the object.
     *
     * @param event DragEvent.DragDropped
     * @param scene
     */
    public static void onDragDropped(DragEvent event, Scene scene) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            final List<File> files = db.getFiles();
            Task task = new Task() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(() -> {
//                        ProgressStatus ps1 = new ProgressStatus("Receiving Data Drop...", -1);
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, null));
                    });
                    for (File file : files) {
                        try {
                            System.out.println("Drag dropped!");
                        } catch (Exception ex) {
                            LOG.error(null, ex);
                        }
                    }
                    Platform.runLater(() -> {
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
                    });
                    return null;
                }
            };
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
            event.setDropCompleted(true);
            event.consume();
        }
    }

    public static String detectDropType(DragEvent event) {
        Dragboard db = event.getDragboard();
        String type = "UNKNOWN";
        if (db.hasFiles()) {
            final File file = db.getFiles().get(0);
            try {
//                if (JavaFX3DUtils.isTextureFile(file)) {
//                    type = "Hypersurface";
//                } else if (isAudioFile(file)) {
//                    type = "Hypersurface";
//                } else if (FeatureCollectionFile.isFeatureCollectionFile(file)) {
                    type = "Hyperspace";
            } catch (Exception ex) {
                LOG.error(null, ex);
            }
        }
        return type;
    }

    public static String removeExtension(String filename) {
        return filename.substring(0, filename.lastIndexOf("."));
    }

    public static String getNameFromURI(String uriString) {
        try {
            return Paths.get(new URI(uriString)).getFileName().toString();
        } catch (URISyntaxException ex) {
            LOG.error("Could not load URI from: " + uriString);
        }
        return "";
    }
//
//    public static Image bytesToImage(byte[] image) {
//        byte[] rayray = new byte[image.length];
//        System.arraycopy(image, 0,
//            rayray, 0, rayray.length);
//        Image imageObject = new Image(new ByteArrayInputStream(rayray));
//        return imageObject;
//    }
//
//    /**
//     * https://stackoverflow.com/questions/24038524/how-to-get-byte-from-javafx-imageview
//     *
//     * @param image
//     * @return byte [] da bytes
//     */
//    public static byte[] imageToBytes(Image image) throws IOException {
//        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
//        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", byteOutput);
//        return byteOutput.toByteArray();
//    }
//
//    public static byte[] byteMe(Image me) throws IOException {
//        int w = (int) me.getWidth();
//        int h = (int) me.getHeight();
//        int[] intBuf = new int[w * h];
//        me.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), intBuf, 0, w);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        DataOutputStream dos = new DataOutputStream(baos);
//        for (int i = 0; i < intBuf.length; ++i) {
//            dos.writeInt(intBuf[i]);
//        }
//        return baos.toByteArray();
//    }
//
//    public static String imageToBase64(Image image) throws IOException {
//        return Base64.getEncoder().encodeToString(imageToBytes(image));
//    }
}
