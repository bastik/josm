/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.codec.binary;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;

/**
 * Provides Base32 encoding and decoding as defined by RFC 4648.
 * 
 *                               <b>Initial implementation. API may change. Incomplete.</b>
 *  
 * <p>
 * The class can be parameterized in the following manner with various constructors:
 * <ul>
 * <li>Line length: Default 76. Line length that aren't multiples of 8 will still essentially end up being multiples of
 * 8 in the encoded data.
 * 
 * <li>Line separator: Default is CRLF ("\r\n")</li>
 * </ul>
 * </p>
 * <p>
 * Since this class operates directly on byte streams, and not character streams, it is hard-coded to only encode/decode
 * character encodings which are compatible with the lower 127 ASCII chart (ISO-8859-1, Windows-1252, UTF-8, etc).
 * </p>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc4648.txt">RFC 4648</a>
 * 
 * @since 1.5
 * @version $Revision: 1063657 $
 */
public class Base32 implements BinaryEncoder, BinaryDecoder {

    private static final int DEFAULT_BUFFER_RESIZE_FACTOR = 2;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * BASE32 characters are 5 bits in length. 
     * They are formed by taking a block of five octets to form a 40-bit string, 
     * which is converted into eight BASE32 characters.
     */
    private static final int BITS_PER_ENCODED_CHAR = 5;
    private static final int BYTES_PER_UNENCODED_BLOCK = 5;
    private static final int BYTES_PER_ENCODED_BLOCK = 8;


    /**
     *  MIME chunk size per RFC 2045 section 6.8.
     * 
     * <p>
     * The {@value} character limit does not count the trailing CRLF, but counts all other characters, including any
     * equal signs.
     * </p>
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 section 6.8</a>
     */
    public static final int MIME_CHUNK_SIZE = 76;

    /**
     * PEM chunk size per RFC 1421 section 4.3.2.4.
     * 
     * <p>
     * The {@value} character limit does not count the trailing CRLF, but counts all other characters, including any
     * equal signs.
     * </p>
     * 
     * @see <a href="http://tools.ietf.org/html/rfc1421">RFC 1421 section 4.3.2.4</a>
     */
    public static final int PEM_CHUNK_SIZE = 64;

    /**
     * Chunk separator per RFC 2045 section 2.1.
     *
     * <p>
     * N.B. The next major release may break compatibility and make this field private.
     * </p>
     * 
     * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 section 2.1</a>
     */
    private static final byte[] CHUNK_SEPARATOR = {'\r', '\n'};

    /**
     * This array is a lookup table that translates 5-bit positive integer index values into their "Base32 Alphabet"
     * equivalents as specified in Table 3 of RFC 2045.
     */
    private static final byte[] STANDARD_ENCODE_TABLE = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '2', '3', '4', '5', '6', '7',
    };

    /**
     * Byte used to pad output.
     */
    private static final byte PAD = '=';

    /**
     * This array is a lookup table that translates Unicode characters drawn from the "Base32 Alphabet" (as specified in
     * Table 3 of RFC 2045) into their 5-bit positive integer equivalents. Characters that are not in the Base32
     * alphabet but fall within the bounds of the array are translated to -1.
     * 
     */
    private static final byte[] DECODE_TABLE = {
         //  0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 00-0f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 10-1f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 63, // 20-2f
            -1, -1, 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, // 30-3f 2-7
            -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, // 40-4f A-N
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,                     // 50-5a O-Z
    };

    /** Mask used to extract 5 bits, used when encoding Base32 bytes */
    private static final int MASK_5BITS = 0x1f;

    /** Mask used to extract 8 bits, used in decoding Base32 bytes */
    private static final int MASK_8BITS = 0xff;

    // The static final fields above are used for the original static byte[] methods on Base32.
    // The private member fields below are used with the new streaming approach, which requires
    // some state be preserved between calls of encode() and decode().

    /**
     * Encode table to use.
     */
    private final byte[] encodeTable;

    /**
     * Line length for encoding. Not used when decoding. A value of zero or less implies no chunking of the Base32
     * encoded data.
     */
    private final int lineLength;

    /**
     * Line separator for encoding. Not used when decoding. Only used if lineLength > 0.
     */
    private final byte[] lineSeparator;

    /**
     * Convenience variable to help us determine when our buffer is going to run out of room and needs resizing.
     * <code>decodeSize = 3 + lineSeparator.length;</code>
     * TODO 3?
     */
    private final int decodeSize;

    /**
     * Convenience variable to help us determine when our buffer is going to run out of room and needs resizing.
     * <code>encodeSize = 4 + lineSeparator.length;</code>
     * TODO 4? is that BYTES_PER_ENCODED_BLOCK? - probably yes
     */
    private final int encodeSize;

    /**
     * Buffer for streaming.
     */
    private byte[] buffer;

    /**
     * Position where next character should be written in the buffer.
     */
    private int pos;

    /**
     * Position where next character should be read from the buffer.
     */
    private int readPos;

    /**
     * Variable tracks how many characters have been written to the current line. Only used when encoding. We use it to
     * make sure each encoded line never goes beyond lineLength (if lineLength > 0).
     */
    private int currentLinePos;

    /**
     * Writes to the buffer only occur after every 5 reads when encoding, and every 8 reads when decoding. This variable
     * helps track that.
     */
    private int modulus;

    /**
     * Boolean flag to indicate the EOF has been reached. Once EOF has been reached, this Base32 object becomes useless,
     * and must be thrown away.
     */
    private boolean eof;

    /**
     * Place holder for the 8 bytes we're dealing with for our Base32 logic. Bitwise operations store and extract the
     * Base32 encoding or decoding from this variable.
     */
    private long x; // 64 bits; enough for 40 bits i.e. 5 octets unencoded

    /**
     * Creates a Base32 codec used for decoding and encoding.
     * <p>
     * When encoding the line length is 0 (no chunking).
     * </p>
     * 
     */
    public Base32() {
        this(0);
    }

    /**
     * Creates a Base32 codec used for decoding and encoding.
     * <p>
     * When encoding the line length is given in the constructor, the line separator is CRLF.
     * </p>
     * 
     * @param lineLength
     *            Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 8).
     *            If lineLength <= 0, then the output will not be divided into lines (chunks). Ignored when decoding.
     */
    public Base32(int lineLength) {
        this(lineLength, CHUNK_SEPARATOR);
    }

    /**
     * Creates a Base32 codec used for decoding and encoding.
     * <p>
     * When encoding the line length and line separator are given in the constructor.
     * </p>
     * <p>
     * Line lengths that aren't multiples of 8 will still essentially end up being multiples of 8 in the encoded data.
     * </p>
     * 
     * @param lineLength
     *            Each line of encoded data will be at most of the given length (rounded down to nearest multiple of 8).
     *            If lineLength <= 0, then the output will not be divided into lines (chunks). Ignored when decoding.
     * @param lineSeparator
     *            Each line of encoded data will end with this sequence of bytes.
     * @throws IllegalArgumentException
     *             The provided lineSeparator included some Base32 characters. That's not going to work!
     */
    public Base32(int lineLength, byte[] lineSeparator) {
        if (lineSeparator == null) {
            lineLength = 0;  // disable chunk-separating
            lineSeparator = CHUNK_SEPARATOR;  // this just gets ignored
        }
        this.lineLength = lineLength > 0 ? (lineLength / BYTES_PER_ENCODED_BLOCK) * BYTES_PER_ENCODED_BLOCK : 0;
        this.lineSeparator = new byte[lineSeparator.length];
        System.arraycopy(lineSeparator, 0, this.lineSeparator, 0, lineSeparator.length);
        if (lineLength > 0) {
            this.encodeSize = BYTES_PER_ENCODED_BLOCK + lineSeparator.length;
        } else {
            this.encodeSize = BYTES_PER_ENCODED_BLOCK;
        }
        this.decodeSize = this.encodeSize - 1;
        if (containsBase32Byte(lineSeparator)) {
            String sep = StringUtils.newStringUtf8(lineSeparator);
            throw new IllegalArgumentException("lineSeperator must not contain Base32 characters: [" + sep + "]");
        }
        this.encodeTable = STANDARD_ENCODE_TABLE; // TODO - encodeTable could perhaps be removed, but might be useful if merging with Base64
    }

    /**
     * Returns true if this Base32 object has buffered data for reading.
     * 
     * @return true if there is Base32 object still available for reading.
     */
    boolean hasData() {
        return this.buffer != null;
    }

    /**
     * Returns the amount of buffered data available for reading.
     * 
     * @return The amount of buffered data available for reading.
     */
    int avail() {
        return buffer != null ? pos - readPos : 0;
    }

    /** Doubles our buffer. */
    private void resizeBuffer() {
        if (buffer == null) {
            buffer = new byte[DEFAULT_BUFFER_SIZE];
            pos = 0;
            readPos = 0;
        } else {
            byte[] b = new byte[buffer.length * DEFAULT_BUFFER_RESIZE_FACTOR];
            System.arraycopy(buffer, 0, b, 0, buffer.length);
            buffer = b;
        }
    }

    /**
     * Extracts buffered data into the provided byte[] array, starting at position bPos, up to a maximum of bAvail
     * bytes. Returns how many bytes were actually extracted.
     * 
     * @param b
     *            byte[] array to extract the buffered data into.
     * @param bPos
     *            position in byte[] array to start extraction at.
     * @param bAvail
     *            amount of bytes we're allowed to extract. We may extract fewer (if fewer are available).
     * @return The number of bytes successfully extracted into the provided byte[] array.
     */
    int readResults(byte[] b, int bPos, int bAvail) {
        if (buffer != null) {
            int len = Math.min(avail(), bAvail);
            System.arraycopy(buffer, readPos, b, bPos, len);
            readPos += len;
            if (readPos >= pos) {
                buffer = null;
            }
            return len;
        }
        return eof ? -1 : 0;
    }

    /**
     * <p>
     * Encodes all of the provided data, starting at inPos, for inAvail bytes. Must be called at least twice: once with
     * the data to encode, and once with inAvail set to "-1" to alert encoder that EOF has been reached, so flush last
     * remaining bytes (if not multiple of 5).
     * </p>
     * 
     * @param in
     *            byte[] array of binary data to Base32 encode.
     * @param inPos
     *            Position to start reading data from.
     * @param inAvail
     *            Amount of bytes available from input for encoding.
     */
    void encode(byte[] in, int inPos, int inAvail) {
        if (eof) {
            return;
        }
        // inAvail < 0 is how we're informed of EOF in the underlying data we're
        // encoding.
        if (inAvail < 0) {
            eof = true;
            if (buffer == null || buffer.length - pos < encodeSize) {
                resizeBuffer();
            }
            switch (modulus) { // % 5
                case 1 : // Only 1 octet; take top 5 bits then remainder
                    buffer[pos++] = encodeTable[(int)(x >> 3) & MASK_5BITS]; // 8-1*5 = 3
                    buffer[pos++] = encodeTable[(int)(x << 2) & MASK_5BITS]; // 5-3=2
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    break;

                case 2 : // 2 octets = 16 bits to use
                    buffer[pos++] = encodeTable[(int)(x >> 11) & MASK_5BITS]; // 16-1*5 = 11
                    buffer[pos++] = encodeTable[(int)(x >>  6) & MASK_5BITS]; // 16-2*5 = 6
                    buffer[pos++] = encodeTable[(int)(x >>  1) & MASK_5BITS]; // 16-3*5 = 1
                    buffer[pos++] = encodeTable[(int)(x <<  4) & MASK_5BITS]; // 5-1 = 4
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    break;
                case 3 : // 3 octets = 24 bits to use
                    buffer[pos++] = encodeTable[(int)(x >> 19) & MASK_5BITS]; // 24-1*5 = 19
                    buffer[pos++] = encodeTable[(int)(x >> 14) & MASK_5BITS]; // 24-2*5 = 14
                    buffer[pos++] = encodeTable[(int)(x >>  9) & MASK_5BITS]; // 24-3*5 = 9
                    buffer[pos++] = encodeTable[(int)(x >>  4) & MASK_5BITS]; // 24-4*5 = 4
                    buffer[pos++] = encodeTable[(int)(x <<  1) & MASK_5BITS]; // 5-4 = 1
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    buffer[pos++] = PAD;
                    break;
                case 4 : // 4 octets = 32 bits to use
                    buffer[pos++] = encodeTable[(int)(x >> 27) & MASK_5BITS]; // 32-1*5 = 27
                    buffer[pos++] = encodeTable[(int)(x >> 22) & MASK_5BITS]; // 32-2*5 = 22
                    buffer[pos++] = encodeTable[(int)(x >> 17) & MASK_5BITS]; // 32-3*5 = 17
                    buffer[pos++] = encodeTable[(int)(x >> 12) & MASK_5BITS]; // 32-4*5 = 12
                    buffer[pos++] = encodeTable[(int)(x >>  7) & MASK_5BITS]; // 32-5*5 =  7
                    buffer[pos++] = encodeTable[(int)(x >>  2) & MASK_5BITS]; // 32-6*5 =  2
                    buffer[pos++] = encodeTable[(int)(x <<  3) & MASK_5BITS]; // 5-2 = 3
                    buffer[pos++] = PAD;
                    break;
            }
            // Don't want to append the CRLF two times in a row, so make sure previous
            // character is not from CRLF!
            byte b = lineSeparator[lineSeparator.length - 1];
            if (lineLength > 0 && pos > 0 && buffer[pos-1] != b) {
                System.arraycopy(lineSeparator, 0, buffer, pos, lineSeparator.length);
                pos += lineSeparator.length;
            }
        } else {
            for (int i = 0; i < inAvail; i++) {
                if (buffer == null || buffer.length - pos < encodeSize) {
                    resizeBuffer();
                }
                modulus = (++modulus) % BITS_PER_ENCODED_CHAR;
                int b = in[inPos++];
                if (b < 0) {
                    b += 256;
                }
                x = (x << 8) + b; // ??
                if (0 == modulus) { // we have enough bytes to create our output 
                    buffer[pos++] = encodeTable[(int)(x >> 35) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)(x >> 30) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)(x >> 25) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)(x >> 20) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)(x >> 15) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)(x >> 10) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)(x >> 5) & MASK_5BITS];
                    buffer[pos++] = encodeTable[(int)x & MASK_5BITS];
                    currentLinePos += BYTES_PER_ENCODED_BLOCK;
                    if (lineLength > 0 && lineLength <= currentLinePos) {
                        System.arraycopy(lineSeparator, 0, buffer, pos, lineSeparator.length);
                        pos += lineSeparator.length;
                        currentLinePos = 0;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Decodes all of the provided data, starting at inPos, for inAvail bytes. Should be called at least twice: once
     * with the data to decode, and once with inAvail set to "-1" to alert decoder that EOF has been reached. The "-1"
     * call is not necessary when decoding, but it doesn't hurt, either.
     * </p>
     * <p>
     * Ignores all non-Base32 characters. This is how chunked (e.g. 76 character) data is handled, since CR and LF are
     * silently ignored, but has implications for other bytes, too. This method subscribes to the garbage-in,
     * garbage-out philosophy: it will not check the provided data for validity.
     * </p>
     * 
     * @param in
     *            byte[] array of ascii data to Base32 decode.
     * @param inPos
     *            Position to start reading data from.
     * @param inAvail
     *            Amount of bytes available from input for encoding.
     *
     * Output is written to {@link #buffer} as 8-bit octets, using {@link pos} as the buffer position
     */
    void decode(byte[] in, int inPos, int inAvail) { // package protected for access from I/O streams
        if (eof) {
            return;
        }
        if (inAvail < 0) {
            eof = true;
        }
        for (int i = 0; i < inAvail; i++) {
            if (buffer == null || buffer.length - pos < decodeSize) {
                resizeBuffer();
            }
            byte b = in[inPos++];
            if (b == PAD) {
                // We're done.
                eof = true;
                break;
            } else {
                if (b >= 0 && b < DECODE_TABLE.length) {
                    int result = DECODE_TABLE[b];
                    if (result >= 0) {
                        modulus = (++modulus) % BYTES_PER_ENCODED_BLOCK;
                        x = (x << BITS_PER_ENCODED_CHAR) + result; // collect decoded bytes
                        if (modulus == 0) { // we can output the 5 bytes
                            buffer[pos++] = (byte) ((x >> 32) & MASK_8BITS);
                            buffer[pos++] = (byte) ((x >> 24) & MASK_8BITS);
                            buffer[pos++] = (byte) ((x >> 16) & MASK_8BITS);
                            buffer[pos++] = (byte) ((x >> 8) & MASK_8BITS);
                            buffer[pos++] = (byte) (x & MASK_8BITS);
                        }
                    }
                }
            }
        }

        // Two forms of EOF as far as Base32 decoder is concerned: actual
        // EOF (-1) and first time '=' character is encountered in stream.
        // This approach makes the '=' padding characters completely optional.
        if (eof && modulus != 0) {
            if (buffer == null || buffer.length - pos < decodeSize) {
                resizeBuffer();
            }

            //  we ignore partial bytes, i.e. only multiples of 8 count
            switch (modulus) {
                case 2 : // 10 bits, drop 2 and output one byte
                    buffer[pos++] = (byte) ((x >> 2) & MASK_8BITS);
                    break;
                case 3 : // 15 bits, drop 7 and output 1 byte
                    buffer[pos++] = (byte) ((x >> 7) & MASK_8BITS);
                    break;
                case 4 : // 20 bits = 2*8 + 4
                    x = x >> 4; // drop 4 bits
                    buffer[pos++] = (byte) ((x >> 8) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x) & MASK_8BITS);
                    break;
                case 5 : // 25bits = 3*8 + 1
                    x = x >> 1;
                    buffer[pos++] = (byte) ((x >> 16) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x >> 8) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x) & MASK_8BITS);
                    break;
                case 6 : // 30bits = 3*8 + 6
                    x = x >> 6;
                    buffer[pos++] = (byte) ((x >> 16) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x >> 8) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x) & MASK_8BITS);
                    break;
                case 7 : // 35 = 4*8 +3
                    x = x >> 3;
                    buffer[pos++] = (byte) ((x >> 24) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x >> 16) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x >> 8) & MASK_8BITS);
                    buffer[pos++] = (byte) ((x) & MASK_8BITS);
                    break;
            }
        }
    }

    /**
     * Returns whether or not the <code>octet</code> is in the Base32 alphabet.
     * 
     * @param octet
     *            The value to test
     * @return <code>true</code> if the value is defined in the the Base32 alphabet (or pad), <code>false</code> otherwise.
     */
    public static boolean isBase32(byte octet) {
        return octet == PAD || (octet >= 0 && octet < DECODE_TABLE.length && DECODE_TABLE[octet] != -1);
    }

    /**
     * Tests a given String to see if it contains only valid characters within the Base32 alphabet. Currently the
     * method treats whitespace as valid.
     * 
     * @param base32
     *            String to test
     * @return <code>true</code> if all characters in the String are valid characters in the Base32 alphabet or if
     *         the String is empty; <code>false</code>, otherwise
     */
    public static boolean isBase32(String base32) {
        return isBase32(StringUtils.getBytesUtf8(base32));
    }

    /**
     * Tests a given byte array to see if it contains only valid characters within the Base32 alphabet. Currently the
     * method treats whitespace as valid.
     * 
     * @param arrayOctet
     *            byte array to test
     * @return <code>true</code> if all bytes are valid characters in the Base32 alphabet or if the byte array is empty;
     *         <code>false</code>, otherwise
     */    
    public static boolean isBase32(byte[] arrayOctet) {
        for (int i = 0; i < arrayOctet.length; i++) {
            if (!isBase32(arrayOctet[i]) && !isWhiteSpace(arrayOctet[i])) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Tests a given byte array to see if it contains only valid characters within the Base32 alphabet.
     * Does not allow white-space.
     * 
     * @param arrayOctet
     *            byte array to test
     * @return <code>true</code> if any byte is a valid character in the Base32 alphabet; <code>false</code> otherwise
     */
    private static boolean containsBase32Byte(byte[] arrayOctet) {
        for (int i = 0; i < arrayOctet.length; i++) {
            if (isBase32(arrayOctet[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encodes binary data using the Base32 algorithm but does not chunk the output.
     * 
     * @param binaryData
     *            binary data to encode
     * @return byte[] containing Base32 characters in their UTF-8 representation.
     */
    public static byte[] encodeBase32(byte[] binaryData) {
        return encodeBase32(binaryData, false);
    }

    /**
     * Encodes binary data using the Base32 algorithm but does not chunk the output.
     *
     * @param binaryData
     *            binary data to encode
     * @return String containing Base32 characters.
     */    
    public static String encodeBase32String(byte[] binaryData) {
        return StringUtils.newStringUtf8(encodeBase32(binaryData, false));
    }
    
    /**
     * Encodes binary data using the Base32 algorithm and chunks the encoded output into 76 character blocks
     * 
     * @param binaryData
     *            binary data to encode
     * @return Base32 characters chunked in 76 character blocks
     */
    public static byte[] encodeBase32Chunked(byte[] binaryData) {
        return encodeBase32(binaryData, true);
    }

    /**
     * Decodes an Object using the Base32 algorithm. This method is provided in order to satisfy the requirements of the
     * Decoder interface, and will throw a DecoderException if the supplied object is not of type byte[] or String.
     * 
     * @param pObject
     *            Object to decode
     * @return An object (of type byte[]) containing the binary data which corresponds to the byte[] or String supplied.
     * @throws DecoderException
     *             if the parameter supplied is not of type byte[]
     */
    public Object decode(Object pObject) throws DecoderException {        
        if (pObject instanceof byte[]) {
            return decode((byte[]) pObject);
        } else if (pObject instanceof String) {
            return decode((String) pObject);
        } else {
            throw new DecoderException("Parameter supplied to Base32 decode is not a byte[] or a String");
        }
    }

    /**
     * Decodes a String containing characters in the Base32 alphabet.
     *
     * @param pArray
     *            A String containing Base32 character data
     * @return a byte array containing binary data
     * @since 1.4
     */
    public byte[] decode(String pArray) {
        return decode(StringUtils.getBytesUtf8(pArray));
    }

    /**
     * Decodes a byte[] containing characters in the Base32 alphabet.
     * 
     * @param pArray
     *            A byte array containing Base32 character data
     * @return a byte array containing binary data
     */
    public byte[] decode(byte[] pArray) {
        reset();
        if (pArray == null || pArray.length == 0) {
            return pArray;
        }
        decode(pArray, 0, pArray.length);
        decode(pArray, 0, -1); // Notify decoder of EOF.
        byte[] result = new byte[pos];
        readResults(result, 0, result.length);
        return result;
    }

    /**
     * Encodes binary data using the Base32 algorithm, optionally chunking the output into 76 character blocks.
     * 
     * @param binaryData
     *            Array containing binary data to encode.
     * @param isChunked
     *            if <code>true</code> this encoder will chunk the Base32 output into 76 character blocks
     * @return Base32-encoded data.
     * @throws IllegalArgumentException
     *             Thrown when the input array needs an output array bigger than {@link Integer#MAX_VALUE}
     */
    public static byte[] encodeBase32(byte[] binaryData, boolean isChunked) {
        return encodeBase32(binaryData, isChunked, Integer.MAX_VALUE);
    }

    /**
     * Encodes binary data using the Base32 algorithm, optionally chunking the output into 76 character blocks.
     * 
     * @param binaryData
     *            Array containing binary data to encode.
     * @param isChunked
     *            if <code>true</code> this encoder will chunk the Base32 output into 76 character blocks
     * @param maxResultSize
     *            The maximum result size to accept.
     * @return Base32-encoded data.
     * @throws IllegalArgumentException
     *             Thrown when the input array needs an output array bigger than maxResultSize
     * @since 1.4
     */
    public static byte[] encodeBase32(byte[] binaryData, boolean isChunked, int maxResultSize) {
        if (binaryData == null || binaryData.length == 0) {
            return binaryData;
        }

        long len = getEncodeLength(binaryData, MIME_CHUNK_SIZE, CHUNK_SEPARATOR);
        if (len > maxResultSize) {
            throw new IllegalArgumentException("Input array too big, the output array would be bigger (" +
                len +
                ") than the specified maxium size of " +
                maxResultSize);
        }
                
        Base32 b64 = isChunked ? new Base32(MIME_CHUNK_SIZE, CHUNK_SEPARATOR) : new Base32(0, CHUNK_SEPARATOR);
        return b64.encode(binaryData);
    }

    /**
     * Decodes a Base32 String into octets
     *
     * @param base32String
     *            String containing Base32 data
     * @return Array containing decoded data.
     */
    public static byte[] decodeBase32(String base32String) {
        return new Base32().decode(base32String);
    }

    /**
     * Decodes Base32 data into octets
     * 
     * @param base32Data
     *            Byte array containing Base32 data
     * @return Array containing decoded data.
     */
    public static byte[] decodeBase32(byte[] base32Data) {
        return new Base32().decode(base32Data);
    }

    /**
     * Checks if a byte value is whitespace or not.
     * 
     * @param byteToCheck
     *            the byte to check
     * @return true if byte is whitespace, false otherwise
     */
    private static boolean isWhiteSpace(byte byteToCheck) {
        switch (byteToCheck) {
            case ' ' :
            case '\n' :
            case '\r' :
            case '\t' :
                return true;
            default :
                return false;
        }
    }

    // Implementation of the Encoder Interface

    /**
     * Encodes an Object using the Base32 algorithm. This method is provided in order to satisfy the requirements of the
     * Encoder interface, and will throw an EncoderException if the supplied object is not of type byte[].
     * 
     * @param pObject
     *            Object to encode
     * @return An object (of type byte[]) containing the Base32 encoded data which corresponds to the byte[] supplied.
     * @throws EncoderException
     *             if the parameter supplied is not of type byte[]
     */
    public Object encode(Object pObject) throws EncoderException {
        if (!(pObject instanceof byte[])) {
            throw new EncoderException("Parameter supplied to Base32 encode is not a byte[]");
        }
        return encode((byte[]) pObject);
    }

    /**
     * Encodes a byte[] containing binary data, into a String containing characters in the Base32 alphabet.
     *
     * @param pArray
     *            a byte array containing binary data
     * @return A String containing only Base32 character data
     */    
    public String encodeToString(byte[] pArray) {
        return StringUtils.newStringUtf8(encode(pArray));
    }

    /**
     * Encodes a byte[] containing binary data, into a byte[] containing characters in the Base32 alphabet.
     * 
     * @param pArray
     *            a byte array containing binary data
     * @return A byte array containing only Base32 character data
     */
    public byte[] encode(byte[] pArray) {
        reset();        
        if (pArray == null || pArray.length == 0) {
            return pArray;
        }
        encode(pArray, 0, pArray.length);
        encode(pArray, 0, -1); // Notify encoder of EOF.
        byte[] buf = new byte[pos - readPos];
        readResults(buf, 0, buf.length);
        return buf;
    }

    /**
     * Pre-calculates the amount of space needed to Base32-encode the supplied array.
     *
     * @param pArray byte[] array which will later be encoded
     * @param chunkSize line-length of the output (<= 0 means no chunking) between each
     *        chunkSeparator (e.g. CRLF).
     * @param chunkSeparator the sequence of bytes used to separate chunks of output (e.g. CRLF).
     *
     * @return amount of space needed to encoded the supplied array.  Returns
     *         a long since a max-len array will require Integer.MAX_VALUE + 33%.
     */
    private static long getEncodeLength(byte[] pArray, int chunkSize, byte[] chunkSeparator) {
        // Base32 always encodes to multiples of 8 (BYTES_PER_ENCODED_CHUNK).
        chunkSize = (chunkSize / BYTES_PER_ENCODED_BLOCK) * BYTES_PER_ENCODED_BLOCK;

        long len = (pArray.length * BYTES_PER_ENCODED_BLOCK) / BYTES_PER_UNENCODED_BLOCK;
        long mod = len % BYTES_PER_ENCODED_BLOCK;
        if (mod != 0) {
            len += BYTES_PER_ENCODED_BLOCK - mod;
        }
        if (chunkSize > 0) {
            boolean lenChunksPerfectly = len % chunkSize == 0;
            len += (len / chunkSize) * chunkSeparator.length;
            if (!lenChunksPerfectly) {
                len += chunkSeparator.length;
            }
        }
        return len;
    }

    /**
     * Resets this Base32 object to its initial newly constructed state.
     */
    private void reset() {
        buffer = null;
        pos = 0;
        readPos = 0;
        currentLinePos = 0;
        modulus = 0;
        eof = false;
    }

}
