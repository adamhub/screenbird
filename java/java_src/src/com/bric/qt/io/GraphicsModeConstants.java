/*
 * @(#)GraphicsModeConstants.java
 *
 * $Date: 2011-05-02 16:01:45 -0500 (Mon, 02 May 2011) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.qt.io;

 /** This is not a public class because I expect to make some significant
  * changes to this project in the next year.
  * <P>Use at your own risk.  This class (and its package) may change in future releases.
  * <P>Not that I'm promising there will be future releases.  There may not be.  :)
  */
class GraphicsModeConstants {
	public static final int COPY = 0x00;
	public static final int DITHER_COPY = 0x40;
	public static final int BLEND = 0x20;
	public static final int TRANSPARENT = 0x24;
	public static final int STRAIGHT_ALPHA = 0x100;
	public static final int PREMUL_WHITE_ALPHA = 0x101;
	public static final int PREMUL_BLACK_ALPHA = 0x102;
	public static final int STRAIGHT_ALPHA_BLEND = 0x104;
	public static final int COMPOSITION = 0x103;
}
