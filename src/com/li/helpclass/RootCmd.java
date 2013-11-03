package com.li.helpclass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RootCmd {
	// 执行linux命令并且输出格式化之后的结果
	protected static Vector execRootCmd(String paramString) {
		Vector localVector = new Vector();
		try {
			Process localProcess = Runtime.getRuntime().exec("su ");// 经过Root处理的android系统即有su命令
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

	// 执行linux命令但不关注结果输出
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

	// 判断机器Android是否已经root，即是否获取root权限
	protected static boolean haveRoot() {

		int i = execRootCmdSilent("echo test"); // 通过执行测试命令来检测
		if (i != -1)
			return true;
		return false;
	}

	// 字符串分割
	public static String[] AlltoSingle(String a) {
		String regEx = "[' ']+"; // 一个或多个空格
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(a);
		String b = m.replaceAll(",").trim();

		String[] arr = b.split(",");
		return arr;

	}

	private static Object localObject;

}
