import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ImageInverter extends JFrame {

    private JPanel buttons;
    private JLabel imageLabel;
    private JButton openFileButton;
    private JButton processThreadButton;
    private JButton undoButton;
    private JButton saveButton;

    private Deque<BufferedImage> previousImages = new LinkedList<BufferedImage>();
    private BufferedImage image;

    public ImageInverter() {
        super("Multithread Image Inverter");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        buttons = new JPanel(new FlowLayout());
        this.add(buttons, BorderLayout.NORTH);

        //TODO: Welcome screen with name of app, to be hidden once user selects image

        imageLabel = new JLabel() {
            @Override
            public void paint(Graphics g) {
                if (image == null)
                    return;
                int w = getWidth(), h = getHeight();
                if (getHeight() * image.getWidth() > getWidth() * image.getHeight())
                    h = image.getHeight() * getWidth() / image.getWidth();
                else
                    w = image.getWidth() * getHeight() / image.getHeight();
                g.drawImage(image, (getWidth() - w) / 2, 0, w, h, null);
            }
        };
        this.add(imageLabel, BorderLayout.CENTER);

        openFileButton = new JButton("Select Image");
        openFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });
        buttons.add(openFileButton);

        processThreadButton = new JButton("Invert image (Background Thread)");
        processThreadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runProcessThread();
            }
        });
        processThreadButton.setEnabled(false);
        buttons.add(processThreadButton);


        undoButton = new JButton("Undo");
        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undoAction();
            }
        });
        undoButton.setEnabled(false);
        buttons.add(undoButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });
        saveButton.setEnabled(false);
        buttons.add(saveButton);
        this.setExtendedState(MAXIMIZED_BOTH);
    }

    protected void undoAction() {
        if (previousImages.isEmpty())
            return;
        image = previousImages.pop();
        imageLabel.repaint();
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        int rv = chooser.showOpenDialog(this);
        if (rv != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        try {
            BufferedImage newImage = ImageIO.read(file);
            if (newImage == null) {
                JOptionPane.showMessageDialog(this, "Selected file is not a valid image");
                return;
            }
            savePrevious();
            image = newImage;
            processThreadButton.setEnabled(true);
            undoButton.setEnabled(true);
            saveButton.setEnabled(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Problem reading image: " + e.getMessage());
        }
        imageLabel.repaint();
    }

    // save file
    // TODO: Allow user to name file and specify path
    private void saveFile() {
        if (image != null) {
            try {
                BufferedImage bi = image;
                File outputfile = new File("savedImage.png"); /* change path */
                ImageIO.write(bi, "png", outputfile);
            } catch (IOException e) {
                //catch exception
            }
        }
    }

    private void runProcessThread() {

        ProgressMonitor monitor = new ProgressMonitor(this, "Processing image...", "", 0, image.getHeight());

        Thread t = new Thread() {

            public void run() {
                savePrevious();
                for (int j = 0; j < image.getHeight(); j++) {
                    if (monitor.isCanceled()) {
                        undoAction();
                        break;
                    }
                    for (int i = 0; i < image.getWidth(); i++) {
                        processPixel(image, image, j, i);
                    }

                    imageLabel.repaint();
                    monitor.setProgress(j + 1);
                }
            }
        };

        t.start();
    }

    private void processPixel(BufferedImage oldImage, BufferedImage newImage, int j, int i) {
        Color oldColor = new Color(oldImage.getRGB(i, j), true),
                newColor = new Color(255 - oldColor.getRed(),
                        255 - oldColor.getGreen(),
                        255 - oldColor.getBlue(),
                        oldColor.getAlpha());
        newImage.setRGB(i, j, newColor.getRGB());
    }

    private void savePrevious() {
        if (image != null)
            previousImages.push(imageClone());
    }

    /**
     * @return
     */
    private BufferedImage imageClone() {
        return new BufferedImage(image.getColorModel(), image.copyData(null),
                image.getColorModel().isAlphaPremultiplied(), null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ImageInverter().setVisible(true);
            }
        });
    }
}


