package com.nmerris;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // get the file path from the user
        // example file path: /home/nmerris/IdeaProjects/bitmap header reader/src/com/nmerris/steg.bmp
        System.out.println("Enter a FULL file path (like '/home/steg.bmp'): ");
        Path filePath = Paths.get(scanner.nextLine());



        try {
            // convert the whole file into an array of bytes
            // NOTE: Files.readAllBytes will close the file for me, with or without errors
            byte[] data = Files.readAllBytes(filePath);

            // create a directory for the output file(s)
            String newDirectoryString = filePath.toFile().getName() + "_Output";
            File directory = new File(newDirectoryString);
            directory.mkdir();

            // wrap it in a little endian byte buffer to make it easier to get at specific byte chunks
            // NOTE: numbers are stored in little-endian format in bitmap headers, so least significant digit is first
            ByteBuffer bb = ByteBuffer.wrap(data);
            // bitmap files are almost always in little endian format
            bb.order(ByteOrder.LITTLE_ENDIAN);

            // calculate the end of this bitmap file using it's header info
            // ie [offset to start of image data] + [image data size] in bytes
            int calcuatedEndOfFile = bb.getInt(10) + bb.getInt(34);

            System.out.println("ALL VALUES ARE IN DECIMAL UNLESS OTHERWISE SPECIFIED");
            System.out.println("Number of bytes in file: " + data.length);
            System.out.println();

            // iterate through all bytes, checking for 'magic' bitmap signatures as we go
            // since the signature is only 2 hex digits, want to make it more likely to avoid false positives by
            // also scanning for 0 at reserved header offset 6 and 8.. every valid bmp should have zeros both places
            // NOTE: could still get false positives, but much less likely by also scanning for reserved bytes
            // NOTE: all bitmaps start with 'BM' ASCII = 424D hex
            for(int i = 1; i < data.length; i++) {
                if(data[i] == 0x4d && data[i - 1] == 0x42 && data[i + 5] == 0 && data[i + 7] == 0) {
                    // found a possible start of a bitmap file

                    // get the offset in bytes, from the beginning of the file
                    int offset = i - 1;
                    System.out.println("Found possible bitmap file start at offset DEC: " + offset);
                    System.out.println(String.format("Found possible bitmap file start at offset HEX: %x", offset));

                    short signature = bb.getShort(offset); // signature is always 4D42 hex = 19778 dec
                    short reservedAtSix = bb.getShort(offset + 6); // always 0
                    short reservedAtEight = bb.getShort(offset + 8); // always 0
                    int offsetToImageDataStart = bb.getInt(offset + 10);
                    int sizeOfHeader = bb.getInt(offset + 14); // 12 for 2.x BMPs, 40 for 3.x, 108 for 4.x
                    int width = bb.getInt(offset + 18); // in px
                    int height = bb.getInt(offset + 22);// in px
                    short numPlanes = bb.getShort(offset + 26); // always 1
                    short bpp = bb.getShort(offset + 28); // 1, 4, 8, 24 (16?) NOTE: 16+ here means no color table
                    int compression = bb.getInt(offset + 30); // 0 for none, 1 for RLE-8, 2 for RLE-4
                    int imageDataSize = bb.getInt(offset + 34);
                    int numColorsInImage = bb.getInt(offset + 46);
                    int numImportantColors = bb.getInt(offset + 50);

//                    System.out.println("signature, should be 19778: " + signature);
//                    System.out.println("reservedAtSix, should be 0: " + reservedAtSix);
//                    System.out.println("reservedAtEight, should be 0: " + reservedAtEight);
//                    System.out.println("offset to start of image data: " + offsetToImageDataStart);
//                    System.out.println("size of bitmap header, should be 40: " + sizeOfHeader);
//                    System.out.println("width: " + width + ", height: " + height);
//                    System.out.println("number of planes, should be 1: " + numPlanes);
//                    System.out.println("num bpp: " + bpp);
//                    System.out.println("compression (0 for none): " + compression);
//                    System.out.println("image data size in bytes: " + imageDataSize);
                    System.out.println("Bitmap file size calculated from it's header, in bytes: " + (offsetToImageDataStart + imageDataSize));
//                    System.out.println("num colors in image: " + numColorsInImage);
//                    System.out.println("num important colors: " + numImportantColors);
//                    System.out.println("THIS IMAGE should end at offset DEC: " + (offset + offsetToImageDataStart + imageDataSize));
//                    System.out.println("THIS IMAGE should end at offset HEX: " + String.format("%x", offset + offsetToImageDataStart + imageDataSize));

                    Path newFilePath = Paths.get(newDirectoryString + "/" + offset + ".bmp");
                    System.out.println("File path of bitmap: " + newFilePath);
//                    System.out.println("file path of bitmap: " + newDirectoryString + "/" + offset + ".bmp");

                    // write out the currently found bitmap file to disk
                    // and print out it's MD5 hash to the console
                    try (FileOutputStream outputStream = new FileOutputStream(newDirectoryString + "/" + offset + ".bmp")) {
                        outputStream.write(data, offset, offsetToImageDataStart + imageDataSize);
                        String md5Hash = DigestUtils.md5Hex(Files.readAllBytes(newFilePath));
                        System.out.println("MD5 hash of file: " + md5Hash);
                    }

                    System.out.println();

                }
            } // end for iteration through all bytes of input bitmap


            // if the file being processed is larger than the bitmap header indicates it should be,
            // dump the remaining bytes into a new file
            if(data.length > calcuatedEndOfFile) {
                // we could do more data processing here, but per requirements, I am just dumping the data to a file
                // one interesting thing you could do here is scan for common 'magic' signatures for various file types
                try (FileOutputStream outputStream = new FileOutputStream(newDirectoryString + "/" + calcuatedEndOfFile + ".unknown")) {
                    outputStream.write(data, calcuatedEndOfFile, data.length - calcuatedEndOfFile);
                    String md5Hash = DigestUtils.md5Hex(Files.readAllBytes(Paths.get(newDirectoryString + "/" + calcuatedEndOfFile + ".unknown")));
                    System.out.println("Found suspicious data starting at offset DEC: " + calcuatedEndOfFile);
                    System.out.println("Found suspicious data starting at offset HEX: " + String.format("%x", calcuatedEndOfFile));
                    System.out.println("File size, in bytes: " + (data.length - calcuatedEndOfFile));
                    System.out.println("File path: " + newDirectoryString + "/" + calcuatedEndOfFile + ".unknown");
                    System.out.println("MD5 hash of file: " + md5Hash);
                }
            }


        } catch (IOException e) {
            System.out.println("That file path did not work, please try again.");
        }
    }
}
