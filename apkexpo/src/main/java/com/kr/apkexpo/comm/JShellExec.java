package com.kr.apkexpo.comm;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JShellExec {

    private Context mContext;

    private static final String SU_SHELL_COMMAND = "su -c sh";
    private static final String GET_UID_COMMAND = "id";
    private static final String COMMANDS_SEPARATOR = "\n";
    private static final String SNEAK_STRING = "<@@SNEAK-STRING@@>";
    private static final String EXIT_COMMAND = "exit";
    private static final String [] AUTO_ADDED_EXECUTE_COMMANDS = {
            String.format("echo \"%s\" >&1", SNEAK_STRING),
            String.format("echo \"%s\" 1>&2", SNEAK_STRING)
    };

    private Process mProcess;
    private DataInputStream mInputStream;
    private DataOutputStream mOutputStream;
    private DataInputStream mErrStream;
    private boolean mHasInput;
    private boolean mHasError;

    public JShellExec() {

        mProcess = null;
        mInputStream = null;
        mOutputStream = null;
        mErrStream = null;
        mHasInput = mHasError = false;
    }

    public void init(Context context) {
        mContext = context;

        beginSession();
    }

    protected void finalize() throws Throwable {

        closeSession();
        super.finalize();
    }

    public boolean testRoot() {
        return execute("chmod 777 " + mContext.getPackageCodePath());
    }

    private boolean isRoot() {

        boolean is_root = false;
        if (hasInit()) {
            try {
                if (execute(GET_UID_COMMAND)) {

                    List<String> output = getStandardOutput();
                    if ((output == null) || (output.isEmpty())) {
                        JDebug.d("Can't get root access or denied by user");
                        return false;
                    }

                    if (!output.toString().contains("uid=0")) {
                        JDebug.d("Root access rejected by user or device isn't rooted");
                        return false;
                    }

                    is_root = true;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return is_root;
    }

    public boolean hasInit() {
        return (mProcess != null);
    }

    public boolean beginSession() {
        
        if (!hasInit()) {
            boolean correct = false;
            try {
                mProcess = Runtime.getRuntime().exec(SU_SHELL_COMMAND);
                mOutputStream = new DataOutputStream(mProcess.getOutputStream());
                mInputStream = new DataInputStream(mProcess.getInputStream());
                mErrStream = new DataInputStream(mProcess.getErrorStream());
                if (! isRoot()) closeSession();
                correct = (mProcess != null);
            }
            catch (Exception e) {
                e.printStackTrace();
                closeSession();
                mProcess = null;
            }
            return correct;
        }
        return true;
    }

    public boolean closeSession() {

        boolean correct = true;
        if (mOutputStream != null) {
            try {
                if (hasInit()) {
                    try {
                        mOutputStream.writeBytes(COMMANDS_SEPARATOR + EXIT_COMMAND + COMMANDS_SEPARATOR);
                        mOutputStream.flush();
                        correct = (mProcess.waitFor() != 255);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                mOutputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mErrStream != null) {
            try {
                mErrStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        mProcess = null;
        mInputStream = null;
        mOutputStream = null;
        mErrStream = null;
        return correct;
    }

    public boolean execute(List<String> commands) {
        boolean correct = false;
        if (hasInit()) {
            correct = true;
            if ((commands != null) && (commands.size() > 0)) {
                try {
                    correct = false;
                    flushOutputStreams();

                    Collections.addAll(commands, AUTO_ADDED_EXECUTE_COMMANDS);
                    
                    for (String command : commands) {
                        mOutputStream.writeBytes(command + COMMANDS_SEPARATOR);
                        mOutputStream.flush();
                    }
                    correct = mHasInput = mHasError = true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return correct;
    }

    public boolean execute(String command) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add(command);
        return execute(commands);
    }

    private void flushOutputStreams() {
        getStandardOutput();
        getErrorOutput();
    }

    private String readLine(DataInputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int letter = in.read();

            if (letter < 0) {
                JDebug.d("readLine: Data truncated");
                break;
            }

            if (letter == 0x0A) break;
            buffer.write(letter);
        }
        return new String(buffer.toByteArray(), "UTF-8");
    }

    private List<String> getOutputStream(DataInputStream stream, boolean include_empty_lines) {

        List<String> data = new ArrayList<>();
        if (stream != null) {
            try {
                String line;
                while (true) {
                    line = readLine(stream);
                    if ((line == null) || (line.equals(SNEAK_STRING))) break;
                    if ((include_empty_lines) || (line.length() > 0)) data.add(line);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public List<String> getStandardOutput() {
        if (mHasInput) {
            mHasInput = false;
            return getOutputStream(mInputStream, true);
        }
        return null;
    }

    public InputStream getStreamOutput() {

        List<String> standOutput = getStandardOutput();
        if (standOutput != null) {

            ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();

            try {
                for (String line : standOutput) arrayStream.write(line.getBytes());
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return new ByteArrayInputStream(arrayStream.toByteArray());
        }

        return null;
    }

    public InputStream rootReadFile(String filePath) {
        return (execute("cat " + filePath)) ? getStreamOutput() : null;
    }

    public List<String> getErrorOutput() {
        if (mHasError) {
            mHasError = false;
            return getOutputStream(mErrStream, false);
        }
        return null;
    }
}
