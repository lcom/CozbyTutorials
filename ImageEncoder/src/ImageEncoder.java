import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class ImageEncoder {

	//////////////////////
	// USER PREFERENCES //
	//////////////////////
	// Whether to write an image or read an image
	static boolean WRITE =			false;
	// Whether to generate an image or use one provided
	static boolean GENERATE_IMAGE =	true;

	// Encoding
	static String MESSAGE_PATH =			"C:\\Users\\Lucas\\Desktop\\message.txt";
	static String IMAGE_TO_ENCODE =			"C:\\Users\\Lucas\\Desktop\\original.png";
	static String OUTPUT_ENCODED_IMAGE =	"C:\\Users\\Lucas\\Desktop\\encoded.png";
	// Decoding
	static String IMAGE_TO_DECODE =			"C:\\Users\\Lucas\\Desktop\\encoded.png";
	
	// Encryption key ... "" = no encryption
	static String KEY = "password";
	// These weights determine the R,G,B cycles of the resulting image
	static int[] weights = new int[] { 3, 6, 5 };

	
	public static void main(String[] args) {
		if (WRITE) {
			makeEncodedImageFile(MESSAGE_PATH, IMAGE_TO_ENCODE, OUTPUT_ENCODED_IMAGE, KEY);
		} else {
			String message = readEncodedImageFile(IMAGE_TO_DECODE, KEY);
			System.out.println(message);
		}
	}
	/**
	 * Saves a generated PNG image with an encoded message.
	 * The image's size is determined by the size of the String input.
	 * The message will be encoded according to the key provided.
	 * Use string "" for no key.
	 * 
	 * @param messageInputPath the input path of the TXT message to encode
	 * @param imageToEncode the path of the image that will be encoded (can be null if generating an image)
	 * @param fileOutputPath the output path of the encoded PNG image to be saved
	 * @param key the encryption key to use. Use "" or null if no key
	 */
	public static void makeEncodedImageFile(String messageInputPath, String imageToEncode, String fileOutputPath, String key) {
		if (key == null){
			key = "";
		}
		String string = readTextFile(messageInputPath);
		int length = string.length();

		BufferedImage image = null;
		if (GENERATE_IMAGE){
			int w = (int) (Math.sqrt(length)) + 1;
			int h = (int) (Math.sqrt(length)) + 1;
			image = makePrettyPicture(w, h);
		}else{
			image = loadImage(imageToEncode);
		}
		encodeImage(image, string, key);
		saveImage(image, fileOutputPath);
	}

	/**
	 * Reads an encoded image from the file system and returns the message encoded inside.
	 * The message will be decrypted using the key provided.
	 * @param fileInputPath the path of the encoded PNG image
	 * @param key the key used to decrypt the image. Use "" if no encryption
	 * @return the encoded message
	 */
	public static String readEncodedImageFile(String fileInputPath, String key) {
		if (key == null){
			key = "";
		}
		BufferedImage image = loadImage(fileInputPath);
		return readEncodedImage(image, key);
	}

	/**
	 * Reads an encoded image from a buffered image and returns the message encoded inside.
	 * The message will be decrypted using the key provided.
	 * 
	 * @param image the encoded image.
	 * @param key the key used to decrypt the image. Use "" if no encryption
	 * @return the encoded message
	 */
	private static String readEncodedImage(BufferedImage image, String key) {
		int w = image.getWidth();
		int h = image.getHeight();
		String s = "";

		// Generate the decryption bytes using the key
		boolean decrypt = false;
		byte[] decryptBytes = null;
		Random decrypter = null;
		if (key != "") {
			decrypt = true;
			decrypter = new Random(KEY.hashCode());
			decryptBytes = new byte[w * h];
			decrypter.nextBytes(decryptBytes);
		}

		// Character counter
		int k = 0;

		mainLoop: for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {

				// Pull the important parts of the color data
				int rgb = image.getRGB(j, i);
				Color color = new Color(rgb);
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();

				int r0 = r & 0b0111;
				int g0 = g & 0b0111;
				int b0 = b & 0b0011;

				// Generate the char from the pixels
				char c = (char) ((r0 << 5) + (g0 << 2) + b0);

				// Decrypt the char
				if (decrypt) {
					c = (char) ((byte) (c) ^ (decryptBytes[k]));
				}

				// If the character is of value 0, the message is over
				if (c == 0) {
					break mainLoop;
				}

				// Add the string
				s += c;

				// Increment the character counter
				k++;
			}
		}
		return s;
	}
	
	/**
	 * Generates an image with radial colors.
	 * 
	 * @param w the width of the image
	 * @param h the height of the image
	 * @return a BufferedImage with the message encoded into it
	 */
	private static BufferedImage makePrettyPicture(int w, int h) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int[] x = new int[3];
		int[] y = new int[3];
		for (int i = 0; i < 3; i++) {
			x[i] = (int) (Math.random() * w);
			y[i] = (int) (Math.random() * h);
		}

		// Loop through each pixel in the image
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {

				// Generate the pixels for the image
				int r = (int) (255 * weights[0] * (distance(j, i, x[0], y[0]) / distance(0, 0, x[1], y[1])));
				int g = (int) (255 * weights[1] * (distance(j, i, x[1], y[1]) / distance(0, 0, x[2], y[2])));
				int b = (int) (255 * weights[2] * (distance(j, i, x[2], y[2]) / distance(0, 0, x[0], y[0])));

				// Scale the color values to the 0-255 range smoothly
				while (r > 255 || r < 0 || g > 255 || g < 0 || b > 255 || b < 0) {
					r = r > 255 ? 255 - (r - 255) : r;
					r = r < 0 ? -r : r;
					g = g > 255 ? 255 - (g - 255) : g;
					g = g < 0 ? -g : g;
					b = b > 255 ? 255 - (b - 255) : b;
					b = b < 0 ? -b : b;
				}
				
				// Put the color into the image
				Color color = new Color(r, g, b);
				int rgb = color.getRGB();
				image.setRGB(j, i, rgb);
			}
		}
		return image;
	}
	
	/**
	 * Encodes an image with a given message, encrypted with the key specified.
	 * 
	 * @param image the image that will contain the message
	 * @param message the message to be encoded
	 * @param key the encryption key
	 */
	private static void encodeImage(BufferedImage image, String message, String key){

		int length = message.length();
		int w = image.getWidth();
		int h = image.getHeight();
		
		// Generate the encryption bytes using the KEY
		Random encrypter = null;
		byte[] encryptBytes = null;
		boolean encrypt = false;
		if (key != "") {
			encrypter = new Random(KEY.hashCode());
			encrypt = true;
			encryptBytes = new byte[w * h];
			encrypter.nextBytes(encryptBytes);
		}
		
		// Character counter
		int k = 0;

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {

				// Pull the important parts of the color data
				int rgb = image.getRGB(j, i);
				Color color = new Color(rgb);
				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();
				// Zero out the last few bits of the color values
				r = (r & ~0b0111);
				g = (g & ~0b0111);
				b = (b & ~0b0011);
		
				// Encode the character
				char c = 0;
		
				if (k < length) {
					// Get the character to use
					c = message.charAt(k);
				}
		
				// Encrypt the character
				if (encrypt) {
					c = (char) ((byte) (c) ^ (encryptBytes[k]));
				}
		
				// Put the char into the color values
				r += (c & 0xE0) >> 5;
				g += (c & 0x1C) >> 2;
				b += (c & 0x03);
		
				// Put the color into the image
				color = new Color(r, g, b);
				rgb = color.getRGB();
				image.setRGB(j, i, rgb);
				k++;
			}
		}
	}

	/**
	 * Returns the distance between two points.
	 * 
	 * @param x0 X coordinate of first point
	 * @param y0 Y coordinate of first point
	 * @param x1 X coordinate of second point
	 * @param y1 Y coordinate of second point
	 * @return distance between the two points
	 */
	private static double distance(int x0, int y0, int x1, int y1) {
		return Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));
	}

	/**
	 * Read in a TXT file from the file system and return the contents as a String.
	 * 
	 * @param filename the file path of the TXT file.
	 * @return the contents of the file as a String.
	 */
	private static String readTextFile(String filename) {
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
		}

		String line = null;
		String total = "";

		try {
			line = br.readLine();
		} catch (IOException e) {
		}

		while (line != null) {
			total += line;
			total += '\n';
			try {
				line = br.readLine();
			} catch (IOException e) {
			}
		}

		try {
			br.close();
		} catch (IOException e) {
		}

		return total;
	}

	/**
	 * Saves a BufferedImage to the file path provided.
	 * 
	 * @param image BufferedImage to be saved.
	 * @param fileName file path to save the image to.
	 */
	private static void saveImage(BufferedImage image, String fileName) {
		try {
			ImageIO.write(image, "PNG", new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load an image from the file system as a BufferedImage.
	 * 
	 * @param fileName the file path of the image to load.
	 * @return the BufferedImage loaded from the file system.
	 */
	private static BufferedImage loadImage(String fileName) {
		try {
			return ImageIO.read(new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
