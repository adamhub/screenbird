/*
 * $RCSfile: JaiI18N.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1178 $
 * $Date: 2011-05-02 16:01:45 -0500 (Mon, 02 May 2011) $
 * $State: Exp $
 */
package javax.media.jai;

//import com.sun.media.jai.util.PropertyUtil;
import java.text.MessageFormat;
import java.util.Locale;

class JaiI18N {
    static String packageName = "javax.media.jai";

    public static String getString(String key) {
        //return PropertyUtil.getString(packageName, key);
        return "";
    }

    public static String formatMsg(String key, Object[] args) {
        MessageFormat mf = new MessageFormat(getString(key));
        mf.setLocale(Locale.getDefault());

        return mf.format(args);
    }
}
