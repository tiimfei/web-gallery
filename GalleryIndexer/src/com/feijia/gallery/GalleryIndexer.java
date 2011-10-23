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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * This is a util to index a picture folder
 * 
 * @author feijia
 * 
 */
public class GalleryIndexer {
	private static Options options = null; // Command line options
	private static final String OPTION_START_FOLDER = "f";
	private static final String OPTION_REGEN_THUMB = "c";
	private static final String OPTION_HELP = "h";
	private static final String OPTION_EXCLUDE = "e";

	static {
		options = new Options();
		options.addOption(OPTION_START_FOLDER, true, "Base folder to index");
		options.addOption(OPTION_REGEN_THUMB, false,
				"Clean and regenerate thumbnails");
		options.addOption(OPTION_HELP, false, "Print this help Message");
		options
				.addOption(OPTION_EXCLUDE, true,
						"Set folder prefix to exclude during indexing, sample \"dojo;base;.\"");
	}
	private static CommandLine cmd = null; // Command Line arguments

	private static final int THUMB_WIDTH = 200;
	private static final int THUMB_HEIGHT = 150;

	private static boolean regenThumb = false;
	private static String startPath = ".";
	private static List<String> excludes = new ArrayList<String>(10);

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

		File[] files = dir.listFiles(new FilenameFilter() {

			/**
			 * Filtering out the folder "exclude" and others like dojo and
			 * utilis
			 */
			@Override
			public boolean accept(File parent, String fn) {
				boolean ret = true;
				ret = ret && !fn.startsWith("exclude");
				Iterator<String> it = excludes.iterator();
				while( it.hasNext()){
					String ex = it.next();
					ret = ret && ! fn.startsWith(ex);
				}
				
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
								+ i.getName() + "\"");
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

	public static void parseArguments(String[] args) {
		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);

			// Check for mandatory args
			if (cmd.hasOption(OPTION_HELP)) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar this_jar.jar", options);
				System.exit(0);
			}
			if (!cmd.hasOption(OPTION_START_FOLDER)) {
				System.out
						.println("Use current folder as default base dir to start indexing");

			} else {
				startPath = cmd.getOptionValue(OPTION_START_FOLDER);
			}
			if (cmd.hasOption(OPTION_REGEN_THUMB)) {
				regenThumb = true;
			}
			if (cmd.hasOption(OPTION_EXCLUDE)) {
				parseExclude(cmd.getOptionValue(OPTION_EXCLUDE));
			}
		} catch (ParseException e) {
			System.err.println("Error parsing arguments");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Parse a string value like "dojo;pattern;xxx" into a String ArrayList
	 * @param optionValue
	 */
	private static void parseExclude(String optionValue) {
	    
		StringTokenizer st = new StringTokenizer(optionValue, ";");
		while(st.hasMoreTokens()){
			excludes.add(st.nextToken());			
		}
		
		
		
	}

	/**
	 * Sole entry point to the class and application. Usage: GalleryIndexer
	 * <startfolder> [-c/--clean]
	 * 
	 * @param args
	 *            Array of String arguments.
	 */
	public static void main(String[] args) {

		if (args.length == 0) {
			System.out
					.println("Usage: java com.feijia.GalleryIndexer <startfolder> [-e \"excludeFolderPattern\"][-c/--clean]");
		}

		parseArguments(args);

		long start = System.currentTimeMillis();

		int imgCount = 0;
		Writer folderJSONWriter = null;
		try {
			File baseDir = new File(startPath);
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