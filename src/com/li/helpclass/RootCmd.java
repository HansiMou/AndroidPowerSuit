package com.li.helpclass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RootCmd {
	// ִ��linux����������ʽ��֮��Ľ��
	protected static Vector execRootCmd(String paramString) {
		Vector localVector = new Vector();
		try {
			Process localProcess = Runtime.getRuntime().exec("su ");// ����Root�����androidϵͳ����su����
			OutputStream localOutputStream = localProcess.getOutputStream();
			DataOutputStream localDataOutputStream = new DataOutputStream(
					localOutputStream);
			InputStream localInputStream = localProcess.getInputStream();
			DataInputStream localDataInputStream = new DataInputStream(
					localInputStream);
			String str1 = String.valueOf(paramString);
			String str2 = str1 + "\n";
			localDataOutputStream.writeBytes(str2);
			localDataOutputStream.flush();

			String str3;
			str3 = localDataInputStream.readLine();
			// Log.i("fe", localDataInputStream.available() + "");
			while (localDataInputStream.available() != 0) {
				if (str3.startsWith(" ")) {
					str3 = str3.substring(1);
				}

				localVector.add(str3 + "\n");
				// Log.i("fe", str3);
				// Log.i("fe", localDataInputStream.available() + "");
				str3 = localDataInputStream.readLine();
			}

			localVector.add(str3);
			localDataOutputStream.writeBytes("exit\n");
			localDataOutputStream.flush();
			localProcess.waitFor();
			return localVector;
		} catch (Exception localException) {
			localException.printStackTrace();
		}
		return localVector;
	}

	// ִ��linux�������ע������
	protected static int execRootCmdSilent(String paramString) {
		try {
			Process localProcess = Runtime.getRuntime().exec("su");
			localObject = localProcess.getOutputStream();
			DataOutputStream localDataOutputStream = new DataOutputStream(
					(OutputStream) localObject);
			String str = String.valueOf(paramString);
			localObject = str + "\n";
			localDataOutputStream.writeBytes((String) localObject);
			localDataOutputStream.flush();
			localDataOutputStream.writeBytes("exit\n");
			localDataOutputStream.flush();
			localProcess.waitFor();
			localObject = localProcess.exitValue();

		} catch (Exception localException) {
			localException.printStackTrace();
		} finally {

		}
		return (Integer) localObject;
	}

	// �жϻ���Android�Ƿ��Ѿ�root�����Ƿ��ȡrootȨ��
	protected static boolean haveRoot() {

		int i = execRootCmdSilent("echo test"); // ͨ��ִ�в������������
		if (i != -1)
			return true;
		return false;
	}

	// �ַ����ָ�
	public static String[] AlltoSingle(String a) {
		String regEx = "[' ']+"; // һ�������ո�
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(a);
		String b = m.replaceAll(",").trim();

		String[] arr = b.split(",");
		return arr;

	}

	private static Object localObject;

}
