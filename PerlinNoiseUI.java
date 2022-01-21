import java.util.LinkedList;
import java.util.List;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.MemoryImageSource;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PerlinNoiseUI
{

    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 2048;
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 1080;
    private static final int MIN_THREADS = 1;
    private static final int MAX_THREADS = 32;
    private static final int MIN_METHOD = 1;
    private static final int MAX_METHOD = 6;
    private static final int MIN_IMAGES = 1;
    private static final int MAX_IMAGES = 1000;
    private static PerlinNoise noise = new PerlinNoise();
    // Cartesian values of the screen
    
    private static final double WIDTH = 7.0;
    private static final double HEIGHT = 7.0;
    private static final double CENTER_X = WIDTH/2;
    private static final double CENTER_Y = HEIGHT/2;

    // Maximum number of iterations before a number is declared in the Perlin set
    public static final int MAX_ITERATIONS = 100;

    // Distance from beyond which a point is not in the set
    public static final double THRESHOLD = 2.0;

    public static void main(String[] args)
    {
        // Make sure we have the right number of arguments
        if (args.length != 5)
        {
            printUsage("Must have 5 command line arguments.");
            System.exit(1);
        }

        // Parse and check the arguments. 
        
        int width, height, numberOfThreads, distModel, numberOfImages;

        try
        {
            
            width = parseInt(args[0], "WIDTH", MIN_WIDTH, MAX_WIDTH);
            height = parseInt(args[1], "Height", MIN_HEIGHT, MAX_HEIGHT);
            numberOfImages = parseInt(args[2], "Images", MIN_IMAGES, MAX_IMAGES);
            numberOfThreads = parseInt(args[3], "threads", MIN_THREADS, MAX_THREADS);
            distModel = parseInt(args[4], "Distribution Model", MIN_METHOD, MAX_METHOD);
        } catch (NumberFormatException ex)
        {
            printUsage(ex.getMessage());
            System.exit(2);
            return; // so java knows variables have been initialized
        }

        // Make space for the image
        int[] imageData = new int[width * height];

        // Start clock
        final Stopwatch watch = new Stopwatch();
        for(int imNum = 0; imNum < numberOfImages; imNum++){
            // Make a threads for drawing. The values are passed to the
            // constructor but we could have made them global.
            final List<PerlinDrawingThread> perlinDrawingThreads = new LinkedList<>();
            for (int threadNumber = 0; threadNumber < numberOfThreads; threadNumber++)
            {
                PerlinDrawingThread thread = new PerlinDrawingThread(imageData, distModel, threadNumber, width, height, numberOfThreads);
                perlinDrawingThreads.add(thread);
                thread.start();
            }

            // Wait for the threads to be done
            for (PerlinDrawingThread t : perlinDrawingThreads)
            {
                try
                {
                    t.join();
                } catch (InterruptedException ex)
                {
                    System.err.println("Execution was Interrupted!");
                }
            }

        }
        // Stop the clock
        System.out.printf("Drawing %d image(s) took %f seconds\n", numberOfImages ,watch.elapsedTime());

        // Show the image
        displayImage(imageData, width, height);
    }

    // Print a given message and some basic usage infomation
    private static void printUsage(String errorMessage)
    {
        System.err.println(errorMessage);
        // System.err.println("The program arguments are:");
        // System.err.printf("\ta: the Perlin set's a constant [%f, %f]\n", MIN_A, MAX_A);
        // System.err.printf("\tb: the Perlin set's b constant [%f, %f]\n", MIN_B, MAX_B);
        // System.err.printf("\tsize: the height and width for the image [%d, %d]\n", MIN_SIZE, MAX_SIZE);
        // System.err.printf("\tthreads: the number of threads to use [%d, %d]\n", MIN_THREADS, MAX_THREADS);
    }

    // Parse the given string s as a double and check that it is within the given range. If not
    // throw a NumberFormatException.
    private static double parseDouble(String s, String name, double min, double max)
    {
        final double result;
        try
        {
            result = Double.parseDouble(s);
        } catch (NumberFormatException ex)
        {
            throw new NumberFormatException(String.format("Value, %s, given for %s is not a number", s, name));
        }

        if (result < min || result > max)
        {
            throw new NumberFormatException(String.format("Value, %f, given for %s is not in the range [%f, %f]",
                    result, name, min, max));
        }

        return result;
    }

    // Parse the given string s as a int and check that it is within the given range. If not
    // throw a NumberFormatException. Very simlaer to parseDouble but I did not think it was
    // worth refactoring.
    private static int parseInt(String s, String name, int min, int max)
    {
        final int result;
        try
        {
            result = Integer.parseInt(s);
        } catch (NumberFormatException ex)
        {
            throw new NumberFormatException(String.format("Value, %s, given for %s is not a number", s, name));
        }

        if (result < min || result > max)
        {
            throw new NumberFormatException(String.format("Value, %d, given for %s is not in the range [%d, %d]",
                    result, name, min, max));
        }

        return result;
    }

    private static void displayImage(int[] imageData, int width, int height)
    {
        SwingUtilities.invokeLater(() ->
        {
            // Make a frame
            JFrame f = new JFrame("Perlin Noise");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Add the drawing panel
            DrawingPanel panel = new DrawingPanel(imageData, width, height);
            f.add(panel);
            panel.setPreferredSize(new Dimension(width, height));
            f.pack();
            f.setResizable(false);
            f.setVisible(true);
        });
    }

    // Return the color a given Cartesian point should be colored. 
    private static int perlinColor(double x, double y)
    {
        
        float value = noise.perlin((float) x, (float) y); 
        // Convert value into Hue, Saturation, and Brightness
        return Color.getHSBColor(value, value, value).getRGB();
    }

    private static double distance(double x, double y)
    {
        return distance(x, y, 0, 0);
    }

    private static double distance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    // Convert the given point (x, y) in graphics coordinates into Cartesian
    // coordinates. This is just a linear transformation.
    private static Point2D.Double convertScreenToCartesian(double x, double y, int screenWidth, int screenHeight)
    {
        return new Point2D.Double(WIDTH / screenWidth * x + CENTER_X - (WIDTH / 2.0),
                -HEIGHT / screenHeight * y + CENTER_Y + (HEIGHT / 2.0));
    }

    // A thread for drawing Perlin Noise, does what it says...
    private static class PerlinDrawingThread extends Thread
    {

        // This thread does not enter into any monitors so calling stop is safe.
        // However, deferred cancellation is still the preferred way of stopping
        // threads. This data member will keep track of if the thread should still
        // be running. It has the volatile keyword to let Java know that it could
        // be modified by another thread.
        private volatile boolean running = true;

        // Copies of the values used for drawing
        
        private final int numberOfThreads, threadNumber;
        private final int distMethod;
        private final int[] buffer;
        private final int width, height;

        public PerlinDrawingThread(int[] buffer, int distMethod, int threadNumber, int width, int height, int numberOfThreads)
        {
            super("Perlin Drawing Thread: " + threadNumber + "/" + numberOfThreads);
            this.buffer = buffer;
            this.threadNumber = threadNumber;
            this.distMethod = distMethod;
            this.width = width;
            this.height = height;
            this.numberOfThreads = numberOfThreads;
            
        }

        public void stopRunning()
        {
            running = false;
        }

        // The drawing code
        @Override
        public void run()
        {
            // Keep drawing rows as long as we are not done and are still running
            blockStride();
        }
        // --RowStride
        public void rowStride(){
            for (int row = threadNumber; running && row < height; row += numberOfThreads)
            {
                for (int column = 0; column < width; column++)
                {
                    final Point2D.Double cartesianPoint = convertScreenToCartesian(column, row, width, height);
                    buffer[row * width + column] = perlinColor(cartesianPoint.getX(), cartesianPoint.getY());
                }
            }
        }
        public void blockStride(){
            int blockSize = 10;
            // for (int block = threadNumber * blockSize; running && block < height; block += blockSize * numberOfThreads)
            // {
            //     System.out.println(block);
            //     for (int row = block; row < height && row < block + blockSize; row++){
            //         for(int col = 0; col < width; col++){
            //             final Point2D.Double cartesianPoint = convertScreenToCartesian(col, row, width, height);
            //             buffer[row * width + col] = perlinColor(cartesianPoint.getX(), cartesianPoint.getY());
            //         }
            //     }
            // }
            
            for(int block = threadNumber * blockSize; running && block < height; block+=blockSize * numberOfThreads){
                
                for(int pix = block; pix < (block * blockSize * width) + (blockSize * width) && pix < width * height; pix++){
                    //if(block == 1 * blockSize) System.out.println(pix);
                    int row, col;
                    row = pix / width;
                    col = pix % width;
                    
                    final Point2D.Double cartesianPoint = convertScreenToCartesian(col, row, width, height);
                    buffer[row * width + col] = perlinColor(cartesianPoint.getX(), cartesianPoint.getY());
                }
            }
        }

    }
    //-DRAWING PANEL-
    private static class DrawingPanel extends JPanel
    {

        private final Image image;

        public DrawingPanel(int[] imageData, int width, int height)
        {
            image = super.createImage(new MemoryImageSource(width, height, imageData, 0, width));
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, this);
        }
    }

    static class Stopwatch
    {
        private final long start;

        public Stopwatch()
        {
            start = System.nanoTime();
        }

        public double elapsedTime()
        {
            return (System.nanoTime()- start) / 1000000000.0;
        }

    }
}