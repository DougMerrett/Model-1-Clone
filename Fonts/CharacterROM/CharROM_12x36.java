// Copyright 2021, Douglas Merrett
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// - Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// - Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
// - My name may not be used to endorse or promote products derived from this
//   software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

//
// Use the font12x36_Standard.txt and font12x36_HiRes.txt font files to generate the
// Intel HEX file for the AT27C1024-45PU character ROM.  The Standard font is at the
// bottom of the ROM (0x0000-3FFF) and HiRes from (0x4000-7FFF) which leaves space
// for 2 more fonts if needed...  Also we are only using the lower 12 bits of the 16
// bit word for the character data.
//
// ROM address lines: A13 A12 A11 A10 A09 A08 A07 A06 A05 A04 A03 A02 A01 A00
//                    |----- Byte value of the -----| |-- Scan line of row -|
//                    |---- character on screen ----| SL5 SL4 SL3 SL2 SL1 SL0
//
// A15 & A14 choose the Font, 0x00 = Standard and 0x01 = HiRes
//
// The ScanLine (ie the ROM contents for each row in the character) goes from 0x00 to
// 0x23 (35 in Decimal = 36 rows), the rest of the 16 bit words from 0x24 to 0x3F are
// unprogrammed, so the output file goes like this:
//
// A13 A12 A11 A10 A09 A08 A07 A06 A05 A04 A03 A02 A01 A00
//   0   0   0   0   0   0   0   0   0   0   0   0   0   0 being the top row of char 0
//   0   0   0   0   0   0   0   0   0   0   0   0   0   1 being the 2nd row of char 0
//  ...
//   0   0   0   0   0   0   0   0   1   0   0   0   1   1 being the 36th row of char 0
//   0   0   0   0   0   0   0   1   0   0   0   0   0   0 being the top row of char 1
//  etc
//
// The Intel HEX format for 16 bit words is the same as 8 bit and the most significant
// byte first, then the least significant byte second.  The format for the file is shown
// in great detail here: https://en.wikipedia.org/wiki/Intel_HEX
//
// So if the 16 bits at address 0x7654 is 0x1234, address 1 is 0x2345, then the order in the
// file is :0476540012342345xx where xx is the checkSum
//          aabbbbccddddddddee where aa is # databytes, bbbb is address, cc is 00 which
//          means data and then dddddddd for the 4 bytes of data and ee for the checkSum
//

public class CharROM_12x36
{
	static PrintWriter romFile;

	// Simple main block calls the generate class below
    public static void main (final String[] args)
    {
		try
		{
			// Create the output file
			romFile = new PrintWriter ("CharacterROM.hex");
		}
		catch (Exception e)
		{
			System.out.println ("An error occurred creating romFile");
			e.printStackTrace ();
			System.exit (1);
    	}

		readFont ("font12x36_Standard.txt", 0);
		readFont ("font12x36_HiRes.txt", 1);
		romFile.println (":00000001FF");		// EOF for the HEX file
		romFile.close ();
    }

    private static void readFont (String fileName, int fontNumber)
    {
		// counters & temp variables
		int charNum  = 0;	// Character within the 256 (0-255)
		int charCol  = 0;	// Col within Character (0-11)
		int charRow  = 0;	// Row within Character (0-35)
		int lineNum  = 0;	// Line number of file for error message
		int word     = 0;	// The 12bit word for the line
		long addr;			// Address for the rom file
		int numWords = 0;   // number of words counter for each line (resets at 32)
		long checkSum = 0;	// Checksum for the line

		// Fill the character font bitmap
		try
		{
			// Open the font file

			// The format is 12 characters of 0 or 1 to set the
			// boolean in the appropriate spot within the bitmap
			// with / as the first character meaning a comment,
			// so ignore the line

			File fontFile = new File (fileName);
			Scanner myReader = new Scanner (fontFile);

			while (myReader.hasNextLine ())
			{
				// Fetch the line of "bits"
				String fontLine = myReader.nextLine ();
				lineNum++;

				// Ignore comments
				if (fontLine.charAt (0) == '/')
				{
					// romFile.println (fontLine);  // Output for debug
					continue;
				}

				// romFile.print (fontLine + "  ");  // Output for debug

				// Convert from characters into the binary array
				word = 0;
				for (charCol = 0; charCol < 12; charCol ++)
				{
					char bitChar = fontLine.charAt (charCol);
					word = word * 2 + (bitChar == '1' ? 1 : 0);
				}

				// Output the word in hex
				if (numWords == 0)
				{
					addr = ((charNum * 64 + charRow) * 2) + (fontNumber * 0x8000);
					romFile.print (":48");			// Start of line and there will be 0x48 bytes (72)
					checkSum = 0x48;				// Start the checkSum calculation off

					romFile.format ("%04X", addr);	// Address for this character in ROM

					checkSum += (addr / 256);		// Add the high byte to the checkSum
					checkSum += (addr & 0xFF);		// Add the low byte to the checkSum

					romFile.print ("00");			// This is a data row
				}

				romFile.format ("%04X", word);
				checkSum += (word / 256);			// Add the high byte to the checkSum
				checkSum += (word & 0xFF);			// Add the low byte to the checkSum
				numWords++;

				// We have finished that row, so get the next one
				// and increment the character pointer if we have
				// completed the 36 rows of this character
				charRow++;
				if (charRow >= 36)
				{
					// calculate the checkSum and output it
					byte check = (byte) checkSum;	// get the least significant byte
					check = (byte) ~check;			// get the one's complement
					check++;						// and add one to get two's complement
					romFile.format ("%02X", check);
					romFile.println ();

					// reset counters
					numWords = 0;
					charRow = 0;
					charNum++;
				}
			}
			myReader.close ();
		}
		catch (FileNotFoundException e)
		{
			System.out.println ("An error occurred during font file '" + fileName + "' read at line " + lineNum);
			e.printStackTrace ();
			System.exit (1);
    	}

        return;
    }
}
