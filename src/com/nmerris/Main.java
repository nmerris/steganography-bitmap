package com.nmerris;

import java.io.IOException;
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
        System.out.println("Enter a FULL file path: ");
        Path filePath = Paths.get(scanner.nextLine());

        try {
            // convert the whole file into an array of bytes
            byte[] data = Files.readAllBytes(filePath);

            // wrap it in a little endian byte buffer to make it easier to get at specific byte chunks
            // NOTE: numbers are stored in little-endian format in bitmap headers, so least significant digit is first
            ByteBuffer bb = ByteBuffer.wrap(data);

//            int numSignatures = 0;
//
//            for(int i = 0; i < data.length; i++) {
//                if(data[i] == 0x42) {
//                    numSignatures++;
//                }
//            }


            bb.order(ByteOrder.LITTLE_ENDIAN);

            // get the width and height: width is 4 bytes, always starts at 18, height always starts at 22




            System.out.println("number of bytes: " + data.length);
            System.out.println();



            for(int i = 1; i < data.length; i++) {
                if(data[i] == 0x4d && data[i - 1] == 0x42) {
                    // found a possible start of a bitmap file

                    // get the offset in bytes, from the beginning of the file
                    int offset = i - 1;
                    System.out.println("Found possible bitmap file start at offset: " + offset);

//                    int width = bb.getInt(18);
//                    int height = bb.getInt(22);
//                    short signature = bb.getShort(0); // signature is always 4D42 hex = 19778 dec
//                    int imageDataSize = bb.getInt(34);
//                    int offsetToImageDataStart = bb.getInt(10);
//                    short bpp = bb.getShort(28); // 1, 4, 8, 24 (16?)
//                    short numPlanes = bb.getShort(26); // always 1
//                    int sizeOfHeader = bb.getInt(14); // always 40
//                    short reservedAtSix = bb.getShort(6); // always 0
//                    short reservedAtEight = bb.getShort(8); // always 0

                    int width = bb.getInt(offset + 18);
                    int height = bb.getInt(offset + 22);
                    short signature = bb.getShort(offset); // signature is always 4D42 hex = 19778 dec
                    int imageDataSize = bb.getInt(offset + 34);
                    int offsetToImageDataStart = bb.getInt(offset + 10);
                    short bpp = bb.getShort(offset + 28); // 1, 4, 8, 24 (16?)
                    short numPlanes = bb.getShort(offset + 26); // always 1
                    int sizeOfHeader = bb.getInt(offset + 14); // always 40
                    short reservedAtSix = bb.getShort(offset + 6); // always 0
                    short reservedAtEight = bb.getShort(offset + 8); // always 0

                    System.out.println("signature, should be 19778: " + signature);
                    System.out.println("reservedAtSix, should be 0: " + reservedAtSix);
                    System.out.println("reservedAtEight, should be 0: " + reservedAtEight);
                    System.out.println("offset to start of image data: " + offsetToImageDataStart);
                    System.out.println("size of bitmap header, should be 40: " + sizeOfHeader);
                    System.out.println("width: " + width + ", height: " + height);
                    System.out.println("number of planes, should be 1: " + numPlanes);
                    System.out.println("num bpp: " + bpp);
                    System.out.println("image data size in bytes: " + imageDataSize);
//                    System.out.println("NEXT IMAGE should start at: " + (offset + offsetToImageDataStart + imageDataSize));
                    System.out.println();




                }
            }




        } catch (IOException e) {
            System.out.println("That file path did not work, please try again.");
        }
    }
}
