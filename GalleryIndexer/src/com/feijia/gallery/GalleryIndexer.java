package com.feijia.gallery;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * This is a util to index a picture folder 
 * @author feijia
 *
 */
public class GalleryIndexer {

	private static final int THUMB_WIDTH = 200;
	private static final int THUMB_HEIGHT = 150;
	private static boolean regenThumb = false;

	private static int traverse(File dir, String basePath,
			Writer folderJSONWriter) {

		String[] children = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File parent, String fn) {
				boolean ret = false;
				ret = ret || fn.endsWith("jpg") || fn.endsWith("JPG");
				ret = ret && !fn.startsWith("thumb_");
				ret = ret && !fn.startsWith("exclude");
				return ret;
			}
		});
		System.out.println("Listing images under: " + basePath);
		processImage(children, basePath, dir.getAbsolutePath());
		int imgCount = children.length;

		File[] files = dir.listFiles(new FilenameFilter(){

			/**
			 * Filtering out the folder "exclude" and others like dojo and utilis
			 */
			@Override
			public boolean accept(File parent, String fn) {
				boolean ret = true;
				ret = ret && !fn.startsWith("exclude");
				ret = ret && !fn.startsWith("dojo");
				return ret;
			}
			
		});

		for (File i : files) {
			if (i.isDirectory()) {
				int count = traverse(i, basePath + "/" + i.getName(),
						folderJSONWriter);
				imgCount += count;
				if (count > 0) {
					// none empty folder, add it to folder.json
					try {
						folderJSONWriter.write("{");

						folderJSONWriter.write("\"name\":\"" + basePath + "/"
								+ i.getName()+"\"");
						folderJSONWriter.write("},\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}

		return imgCount;

	}

	private static void processImage(String[] files, String basePath,
			String absPath) {
		Writer out = null;
		try {
			System.out.println("Creating images.json:" + absPath
					+ "/images.json");
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(absPath + "/images.json"), "UTF-8"));
			out.write("{items:[\n");

			ThumbnailGenerator tg = new ThumbnailGenerator();
			for (String fn : files) {
				String originalFile = absPath + "/" + fn;
				String thumbnailFile = absPath + "/" + "thumb_" + fn;

				File thumbnailTest = new File(thumbnailFile);

				// test if thumbnail already generated
				if (regenThumb || !thumbnailTest.exists()) {
					System.out
							.println("Generate Thumbnail for:" + originalFile);
					tg.transform(originalFile, thumbnailFile, THUMB_WIDTH,
							THUMB_HEIGHT, 0);
					System.out.println("Thumbnail" + thumbnailFile
							+ " generated.");
				}
				out.write("{");
				out.write("\"large\":\"" + basePath + "/" + fn + "\",\n");
				out.write("\"thumb\":\"" + basePath + "/thumb_" + fn + "\",\n");
				out.write("\"title\":\"" + basePath + "/" + fn + "\",\n");
				out.write("},\n");
			}

			out.write("]}");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Sole entry point to the class and application.
	 * 
	 * @param args
	 *            Array of String arguments.
	 */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		if (args.length > 1) {
			if ("-c".equals(args[1]) || "--clean".equals(args[1])) {
				regenThumb = true;
				System.out.println("Thumbnail regeneration turned on. ");
			}
		}
		File baseDir = new File(args[0]);
		int imgCount = 0;
		Writer folderJSONWriter = null;
		try {
			folderJSONWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(baseDir.getAbsolutePath()
							+ "/folders.json"), "UTF-8"));
			folderJSONWriter.write("{items:[");
			imgCount = traverse(baseDir, ".", folderJSONWriter);
			folderJSONWriter.write("]}");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (folderJSONWriter != null)
				try {
					folderJSONWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		long end = System.currentTimeMillis();
		System.out.println(imgCount + " images processed...");
		System.out.println((end - start) / 1000 + " secs used");
	}

}