package cn.yong.easydownloader;

import java.util.Arrays;

/**
 *∑√Œ œÏ”¶
 */
public class DownLoadResponse {
    private boolean success;
    private int lastStatus;
    private String lastCause;
    private String responseContent;
    private byte[] responseBytes;


    public DownLoadResponse(boolean success, int lastStatus, String lastCause, String responseContent, byte[] responseBytes) {
        this.success = success;
        this.lastStatus = lastStatus;
        this.lastCause = lastCause;
        this.responseContent = responseContent;
        this.responseBytes = responseBytes;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(int lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastCause() {
        return lastCause;
    }

    public void setLastCause(String lastCause) {
        this.lastCause = lastCause;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public void setResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
    }


    @Override
    public String toString() {
        return "DownLoadResponse{" +
                "success=" + success +
                ", lastStatus=" + lastStatus +
                ", lastCause='" + lastCause + '\'' +
                ", responseContent='" + responseContent + '\'' +
                '}';
    }
}
