//khuangia@gmail.com

package com.kr.apkexpo.ddx;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Base64;
import android.util.Xml;

import com.kr.apkexpo.comm.JDebug;
import com.kr.apkexpo.comm.JResManager;
import com.kr.apkexpo.comm.JShellExec;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class JDdxStatics {

    private static final String DES_METHOD = "DES";
    private static final String DES_KEY = "DESefun////**";
    private static final String DES_SUFFIX = "$|$$***efun";
    private static final String IV_3DES = "12EFun89";
    private static String MSG_HEX_STRING = "0123456789ABCDEF";

    private Context mContext;

    public JShellExec mShellExec;
    public JResManager mResManager;
    public JDbAccess mDb;

    private static class JDdxCrypt {

        public static String decryptDES(String data, String key) {

            try {
                SecureRandom lsr = new SecureRandom();
                SecretKey skey = SecretKeyFactory.getInstance(DES_METHOD).generateSecret(new DESKeySpec(key.getBytes()));

                Cipher localCipher = Cipher.getInstance(DES_METHOD);
                localCipher.init(2, skey, lsr);

                return new String(localCipher.doFinal(Base64.decode(data, 0)));
            }
            catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                    | InvalidKeySpecException |IllegalBlockSizeException |BadPaddingException e) {

                e.printStackTrace();
                return null;
            }
        }

        public static String decryptEfunData(String data) {

            if (null == data) return null;

            if (!data.endsWith(DES_SUFFIX)) {
                return data;
            }

            return decryptDES(data.replace(DES_SUFFIX, ""), DES_KEY);
        }
    }

    public JDdxStatics() {

        mShellExec = new JShellExec();
        mResManager = new JResManager();
        mDb = new JDbAccess();
    }

    public void init(Context context) {

        mContext = context;

        mResManager.init(context);
        mShellExec.init(context);

        mDb.init(context);
    }

    public void un_init() {
        mShellExec.closeSession();
    }

    public String getDdxDataDir() {

        final PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(JDbAccess.StringDdxName)) {
                return packageInfo.dataDir;
            }
        }

        return null;
    }

    public String getDdxUserConfigPath() {

        String dataDir = getDdxDataDir();
        if (dataDir != null) {
            return dataDir + "/shared_prefs/" + JDbAccess.StringDdxConfigFile;
        }

        return null;
    }

    public boolean isExistsDdx() {
        return (null != getDdxDataDir());
    }

    public String xmlReadValue(InputStream stream, String propName) {

        try {
            XmlPullParser xml = Xml.newPullParser();
            xml.setInput(stream, "UTF-8");

            int eventType;
            while (true) {
                eventType = xml.getEventType();
                if (eventType == XmlPullParser.END_DOCUMENT) break;
                else if (eventType == XmlPullParser.START_TAG) {
                    if (xml.getName().equals("string")) {

                        String prop = xml.getAttributeValue(null, "name");
                        if (prop == null) continue;
                        else if (prop.equals(propName)) {
                            return xml.nextText();
                        }
                    }
                }
                xml.next();
            }
        }
        catch (XmlPullParserException | IOException e) {
            return null;
        }

        return null;
    }

    public void copyFile(InputStream fsrc, String newPath) {

        try {
            int byteRead;
            FileOutputStream fdst = new FileOutputStream(newPath);

            byte[] buffer = new byte[0x500];

            while ( (byteRead = fsrc.read(buffer)) != -1) {
                fdst.write(buffer, 0, byteRead);
            }

            fdst.close();
        }
        catch (Exception e) {
            JDebug.d("e");
        }
    }

    public void copyFile(File srcFile, String newPath) {

        try {
            copyFile(new FileInputStream(srcFile), newPath);
        }
        catch (FileNotFoundException ignored) {

        }
    }

    public boolean tryValidateBinFile(String binFile) {
        return true;
    }

    public boolean tryBackupBinFile() {

        File origFile = new File(mDb.getDdxBinFilePath());
        if (!origFile.exists()) {
            return false;
        }

        String myDataPath = mDb.getMyExtDataDir();
        String myBackupPath = myDataPath + "/" + JDbAccess.StringDdxBinFile;

        File srcFile = new File(myBackupPath);
        if (srcFile.exists() && tryValidateBinFile(myBackupPath)) {
            return true;
        }

        copyFile(origFile, myBackupPath);
        return tryValidateBinFile(myBackupPath);
    }

    public List<String> readDdxDefaultUser(JShellExec exec) {

        List<String> retList = new ArrayList<>();

        String userConfigFile = getDdxUserConfigPath();
        if (userConfigFile != null) {

            InputStream stream = exec.rootReadFile(userConfigFile);
            if (null != stream) {

                boolean marked = false;
                if (stream.markSupported()) {
                    stream.mark(0);
                    marked = true;
                }

                String userName = JDdxCrypt.decryptEfunData (xmlReadValue(stream, "LOGIN_USERNAME"));
                if (null != userName) retList.add(userName);

                if (marked) {
                    try {
                        stream.reset();
                    }
                    catch (IOException e) {
                        stream = exec.rootReadFile(userConfigFile);
                    }
                }
                else {
                    stream = exec.rootReadFile(userConfigFile);
                }

                String password = JDdxCrypt.decryptEfunData (xmlReadValue(stream, "LOGIN_PASSWORD"));
                if (null != password) retList.add(password);
            }
        }

        return retList;
    }

    public String getLastTimeModifyDate() {

        File historyFile = new File(mDb.getWorkFilePath());
        if (!historyFile.exists()) {
            return null;
        }

        String stringTime = null;
        try {
            FileInputStream stream = new FileInputStream(historyFile);
            InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            while (br.ready()) {
                String content = br.readLine();
                if (content.startsWith("time: ")) {
                    stringTime = content.substring(6);
                }
            }

            br.close();
            isr.close();

            return stringTime;
        }
        catch (IOException e) {
            return null;
        }
    }
}
