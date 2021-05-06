package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

class ModelSlotAndSub {
    String subId = null;
    String simSlot = null;
    String iccId = null;
}

class SlotInfo {
    String carrierName = "";
    String number = "";
}

public class SimUtil {
    //判断是否是双卡
    public static boolean isDoubleSim(Context mContext) {
        SubscriptionManager manager = SubscriptionManager.from(mContext);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        int count = manager.getActiveSubscriptionInfoCount();
        return count == 2;
    }

    //通过卡槽id获取sim卡的信息0代码卡槽1，1代表卡槽2
    public static SlotInfo getSlotIdInfo(Context mContext, int slotId) {
        String info = "";
        SlotInfo slotInfo = new SlotInfo();
        SubscriptionManager manager = SubscriptionManager.from(mContext);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return slotInfo;
        }
        SubscriptionInfo subInfo = manager.getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo != null) {
            slotInfo.carrierName = subInfo.getCarrierName().toString();
            slotInfo.number = subInfo.getNumber();
        }
        return slotInfo;
    }

    //判断手机中是否装了双卡
    public static boolean isHasDoubleSim(Context mContext) {
        try {
            if (isDoubleSim(mContext)) {
                List<ModelSlotAndSub> slotAndSubs = getModelSlot(mContext);
                return slotAndSubs != null && slotAndSubs.size() == 2;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    //判断是否拥有该权限
    public static boolean isHasPermission(Context mContext, String permission) {
        if (mContext != null) {
            return ActivityCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }


    //申请该权限
    public static void requestOnePermission(Activity activity, String permission, int permissionCode) {
        if (activity != null) {
            activity.requestPermissions(new String[]{permission}, permissionCode);
        }
    }


    //获取icc用来判断
    public static int getIcc(String iccId, List<ModelSlotAndSub> slotAndSubs) {
        int icc = 0;
        if (slotAndSubs != null && slotAndSubs.size() >= 2 && !TextUtils.isEmpty(iccId)) {
            if (iccId.startsWith(slotAndSubs.get(0).iccId)) {
                icc = 0;
            } else if (iccId.startsWith(slotAndSubs.get(1).iccId)) {
                icc = 1;
            } else {
                if (iccId.equals(slotAndSubs.get(0).subId)) {
                    icc = 0;
                } else if (iccId.equals(slotAndSubs.get(1).subId)) {
                    icc = 1;
                } else {
                    icc = 0;
                }
            }
        }
        return icc;
    }

    /**
     * 获取来自那张卡 即卡1 还是卡2
     *
     * @param mContext Context
     * @param subID String
     * @return getIcc
     */
    public static int getSlot(Context mContext, String subID) {
        return getIcc(subID, getModelSlot(mContext));
    }

    //获取对应的卡槽ID和iccID 关联
    private static List<ModelSlotAndSub> getModelSlot(Context mContext) {
        List<ModelSlotAndSub> data = new ArrayList<>();
        ModelSlotAndSub modelSlotAndSub = null;
        SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            List<SubscriptionInfo> mSubcriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
            if (mSubcriptionInfos != null) {
                for (int i = 0; i < mSubcriptionInfos.size(); i++) {
                    SubscriptionInfo info = mSubcriptionInfos.get(i);
                    if (info != null) {
                        modelSlotAndSub = new ModelSlotAndSub();
                        modelSlotAndSub.subId = info.getSubscriptionId() + "";
                        modelSlotAndSub.simSlot = info.getSimSlotIndex() + 1 + "";
                        modelSlotAndSub.iccId = info.getIccId();
                        data.add(modelSlotAndSub);
                        //id=1, iccId=898601178[****] simSlotIndex=0
                        //id=2, iccId=898603189[****] simSlotIndex=1

                        //{id=1, iccId=898601178[****] simSlotIndex=0
                        //{id=3, iccId=898600401[****] simSlotIndex=1
                    }
                }
                //qb2019/07/02修改将卡1和卡2的位置旋转过来，如果第一个simSlot大于第二个的话
                if (data != null && data.size() == 2) {
                    if (!TextUtils.isEmpty(data.get(0).simSlot) && !TextUtils.isEmpty(data.get(1).simSlot)) {
                        int simSlot1 = Integer.parseInt(data.get(0).simSlot);
                        int simSlot2 = Integer.parseInt(data.get(1).simSlot);
                        if (simSlot1 > simSlot2) {
                            ModelSlotAndSub modelSlotAndSub1 = data.get(1);
                            data.remove(data.get(1));
                            data.add(0, modelSlotAndSub1);
                        }
                    }
                }
            }
        }
        return data;
    }
}
