import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;


public class Main {

    public static int MultCommand(Session session, String[] cmds){
        ChannelShell channel = null;
        InputStream in = null;
        OutputStream os = null;
        int returnCode = -1;
        try {
            System.out.println("执行如下命令:\n" + String.join("\n", cmds));
            channel = (ChannelShell) session.openChannel("shell");
            in = channel.getInputStream();
            channel.setPty(true);
            channel.connect();
            os = channel.getOutputStream();



            //向网络流中写入数据，类似于直接向网络流fd中直接写入数据   --------->   写完之后会直接产生结果
            //在inStrema中，  以上为多步执行条件
            //单步执行只需要将IO重定向为单步输入 + 单步输出 + IO堵塞，然后一步一步进行即可实心交互式更新
            for (String cmd : cmds) {
                os.write((cmd + "\r\n").getBytes());
                os.flush();
                TimeUnit.SECONDS.sleep(2);
            }

            os.write(("exit" + "\r\n").getBytes());
            os.flush();

            byte[] tmp = new byte[1024];
            while (true) {
                //从输出流中可以读取到数据
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    //返回的是全部的结果
                    //System.out.println(tmp);
                    if (i < 0) {
                        break;
                    }
                    System.out.println(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    returnCode = channel.getExitStatus();
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                os.close();
                in.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            channel.disconnect();
        }
        return returnCode;
    }

    public String SingleCommand(Session session,String command) throws IOException, JSchException {
        //单语句执行  用来获得一些系统信息时，获得数据，写回APP
        //执行ls命令   然后返回结果

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand("ls");
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String result = IOUtils.toString(in, "UTF-8");
        //关闭连接，然后返回执行的数据
        channelExec.disconnect();
        return result;
    }

    public static void main(String[] args) throws JSchException, IOException {
        Remote remote = new Remote();
        remote.setHost("127.0.0.1");
        remote.setPassword("qwer9742");
        Session session = Utils.getSession(remote);
        session.connect();
        if (session.isConnected()) {
            System.out.println("Host({"+ remote.getHost() +"}) connected.");
        }

        MultCommand(session,new String[]{"ls","mkdir test","ls"});
        //断开连接
        session.disconnect();

    }
}
