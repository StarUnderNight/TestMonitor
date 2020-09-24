package com.dkss.testmonitor.util;

import java.util.HashMap;
import java.util.Map;

public class Protocol {

    //盒子和设备
    public static final byte[] ID_BOX_ID =         {0x00,0x01};
    public static final byte[] ID_TIME =         {0x00,0x02};
    public static final byte[] ID_RESPONSE =         {0x00,0x03};
    public static final byte[] ID_BOX_TYPE =         {0x00,0x04};
    public static final byte[] ID_DEV_TYPE =         {0x00,0x05};

    //病人信息
    public static final byte[] ID_PAT_NO =         {0x10,0x00};
    public static final byte[] ID_PAT_SNAME =      {0x10,0x01};
    public static final byte[] ID_PAT_FNAME =      {0x10,0x02};
    public static final byte[] ID_PAT_SEX =        {0x10,0x03};
    public static final byte[] ID_PAT_TYPE =       {0x10,0x04};
    public static final byte[] ID_PAT_PACE =       {0x10,0x05};
    public static final byte[] ID_PAT_ROME_NO =     {0x10,0x06};
    public static final byte[] ID_PAT_BED_NO = {0x10,0x07};
    public static final byte[] ID_PAT_HEIGHT = {0x10,0x08};
    public static final byte[] ID_PAT_WEIGHT = {0x10,0x09};
    public static final byte[] ID_PAT_BLOOD_TYPE = {0x10,0x10};
    public static final byte[] ID_PAT_AGE = {0x10,0x11};

    //ECG

    public static final byte[] PMR_ECG_ARR = {0x10,0x21};
    public static final byte[] ID_ECG_WAVE_I = {0x10,0x23};
    public static final byte[] ID_ECG_WAVE_II = {0x10,0x24};
    public static final byte[] ID_ECG_WAVE_III = {0x10,0x25};
    public static final byte[] ID_ECG_WAVE_AVR = {0x10,0x26};
    public static final byte[] ID_ECG_WAVE_AVL = {0x10,0x27};
    public static final byte[] ID_ECG_WAVE_AVF = {0x10,0x28};
    public static final byte[] ID_ECG_WAVE_V4 = {0x10,0x29};
    public static final byte[] ID_ECG_WAVE_V1 = {0x10,0x30};
    public static final byte[] ID_ECG_WAVE_V2 = {0x10,0x31};
    public static final byte[] ID_ECG_WAVE_V3 = {0x10,0x32};
    public static final byte[] ID_ECG_WAVE_V5 = {0x10,0x33};
    public static final byte[] ID_ECG_WAVE_V6 = {0x10,0x34};
    public static final byte[] ID_ECG_WAVE_NUM = {0x10,0x36};
    public static final byte[] ID_ECG_PVC = {0x10,0x52};
    public static final byte[] ID_ECG_PVC_H = {0x10,0x53};
    public static final byte[] ID_ECG_PVC_L = {0x10,0x54};
    public static final byte[] ID_ECG_HR = {0x10,0x55};
    public static final byte[] ID_ECG_HR_H = {0x10,0x56};
    public static final byte[] ID_ECG_HR_L = {0x10,0x57};
    public static final byte[] ID_ECG_ST_I = {0x10,0x58};
    public static final byte[] ID_ECG_ST_II = {0x10,0x58};
    public static final byte[] ID_ECG_ST_III = {0x10,0x58};
    public static final byte[] ID_ECG_ST_AVR = {0x10,0x58};
    public static final byte[] ID_ECG_ST_AVL = {0x10,0x58};
    public static final byte[] ID_ECG_ST_AVF = {0x10,0x58};
    public static final byte[] ID_ECG_ST_V4 = {0x10,0x58};
    public static final byte[] ID_ECG_ST_V1 = {0x10,0x58};
    public static final byte[] ID_ECG_ST_V2 = {0x10,0x58};
    public static final byte[] ID_ECG_ST_V3 = {0x10,0x58};
    public static final byte[] ID_ECG_ST_V5 = {0x10,0x58};
    public static final byte[] ID_ECG_ST_V6 = {0x10,0x58};

    //SpO2
    public static final byte[] ID_SpO2 = {0x10,0x70};
    public static final byte[] ID_SpO2_H = {0x10,0x71};
    public static final byte[] ID_SpO2_L = {0x10,0x72};
    public static final byte[] ID_SpO2_WAVE_BO = {0x10,0x78};

    //NIBP
    public static final byte[] ID_NIBP_SYS = {0x10,(byte)0x80};
    public static final byte[] ID_NIBP_MEAN = {0x10,(byte)0x81};
    public static final byte[] ID_NIBP_DIA = {0x10,(byte)0x82};
    public static final byte[] ID_NIBP_SYS_H = {0x10,(byte)0x83};
    public static final byte[] ID_NIBP_SYS_L = {0x10,(byte)0x84};
    public static final byte[] ID_NIBP_MEAN_H = {0x10,(byte)0x85};
    public static final byte[] ID_NIBP_MEAN_L = {0x10,(byte)0x86};
    public static final byte[] ID_NIBP_DIA_H = {0x10,(byte)0x87};
    public static final byte[] ID_NIBP_DIA_L = {0x10,(byte)0x88};

    //RESP
    public static final byte[] ID_RESP_RR = {0x10,(byte)0x90};
    public static final byte[] ID_RESP_RR_H = {0x10,(byte)0x91};
    public static final byte[] ID_RESP_RR_L = {0x10,(byte)0x92};
    public static final byte[] ID_RESP_WAVE = {0x10,(byte)0x93};

    //PR
    public static final byte[] ID_PR = {0x11,0x70};
    public static final byte[] ID_PR_H = {0x11,0x71};
    public static final byte[] ID_PR_L = {0x11,0x72};

    //IBP
    public static final byte[] ID_IBP_P1_SYS = {0x11,(byte)0x80};
    public static final byte[] ID_IBP_P1_MEAN = {0x11,(byte)0x81};
    public static final byte[] ID_IBP_P1_DIA = {0x11,(byte)0x82};
    public static final byte[] ID_IBP_P1_PR = {0x11,(byte)0x83};
    public static final byte[] ID_IBP_P1_WAVE = {0x11,(byte)0x84};

    public static final byte[] ID_IBP_P2_SYS = {0x11,(byte)0x85};
    public static final byte[] ID_IBP_P2_MEAN = {0x11,(byte)0x86};
    public static final byte[] ID_IBP_P2_DIA = {0x11,(byte)0x87};
    public static final byte[] ID_IBP_P2_PR = {0x11,(byte)0x88};
    public static final byte[] ID_IBP_P2_WAVE = {0x11,(byte)0x89};

    public static final byte[] ID_IBP_ART_SYS = {0x11,(byte)0x90};
    public static final byte[] ID_IBP_ART_MEAN = {0x11,(byte)0x91};
    public static final byte[] ID_IBP_ART_DIA = {0x11,(byte)0x92};
    public static final byte[] ID_IBP_ART_PR = {0x11,(byte)0x93};
    public static final byte[] ID_IBP_ART_WAVE = {0x11,(byte)0x94};

    public static final byte[] ID_IBP_CVP_SYS = {0x11,(byte)0x95};
    public static final byte[] ID_IBP_CVP_MEAN = {0x11,(byte)0x96};
    public static final byte[] ID_IBP_CVP_DIA = {0x11,(byte)0x97};
    public static final byte[] ID_IBP_CVP_PR = {0x11,(byte)0x98};
    public static final byte[] ID_IBP_CVP_WAVE = {0x11,(byte)0x99};

    public static final byte[] ID_IBP_PA_SYS = {0x12,0x00};
    public static final byte[] ID_IBP_PA_MEAN = {0x12,0x01};
    public static final byte[] ID_IBP_PA_DIA = {0x12,0x02};
    public static final byte[] ID_IBP_PA_PR = {0x12,0x03};
    public static final byte[] ID_IBP_PA_WAVE = {0x12,0x04};

    public static final byte[] ID_IBP_RAP_SYS = {0x12,0x05};
    public static final byte[] ID_IBP_RAP_MEAN = {0x12,0x06};
    public static final byte[] ID_IBP_RAP_DIA = {0x12,0x07};
    public static final byte[] ID_IBP_RAP_PR= {0x12,0x08};
    public static final byte[] ID_IBP_RAP_WAVE = {0x12,0x09};

    public static final byte[] ID_IBP_LAP_SYS = {0x12,0x10};
    public static final byte[] ID_IBP_LAP_MEAN = {0x12,0x11};
    public static final byte[] ID_IBP_LAP_DIA = {0x12,0x12};
    public static final byte[] ID_IBP_LAP_PR = {0x12,0x13};
    public static final byte[] ID_IBP_LAP_WAVE = {0x12,0x14};

    public static final byte[] ID_IBP_ICP_SYS = {0x12,0x15};
    public static final byte[] ID_IBP_ICP_MEAN = {0x12,0x16};
    public static final byte[] ID_IBP_ICP_DIA = {0x12,0x17};
    public static final byte[] ID_IBP_ICP_PR = {0x12,0x18};
    public static final byte[] ID_IBP_ICP_WAVE = {0x12,0x19};

    //TEMP
    public static final byte[] ID_TEMP_T1 = {0x12,0x30};
    public static final byte[] ID_TEMP_T1_H = {0x12,0x31};
    public static final byte[] ID_TEMP_T1_L = {0x12,0x32};
    public static final byte[] ID_TEMP_T2 = {0x12,0x33};
    public static final byte[] ID_TEMP_T2_H = {0x12,0x34};
    public static final byte[] ID_TEMP_T2_L = {0x12,0x35};
    public static final byte[] ID_TEMP_ESO = {0x12,0x36};
    public static final byte[] ID_TEMP_NASO = {0x12,0x39};
    public static final byte[] ID_TEMP_TYMP = {0x12,0x42};
    public static final byte[] ID_TEMP_RECT = {0x12,0x45};
    public static final byte[] ID_TEMP_BLAD = {0x12,0x48};
    public static final byte[] ID_TEMP_SKIN = {0x12,0x51};

    //VOR
    public static final byte[] ID_VOR_0 = {0x20,0x00};
    public static final byte[] ID_VOR_1 = {0x20,0x01};
    public static final byte[] ID_VOR_2 = {0x20,0x02};
    public static final byte[] ID_VOR_3 = {0x20,0x03};
    public static final byte[] ID_VOR_4 = {0x20,0x04};
    public static final byte[] ID_VOR_5 = {0x20,0x05};
    public static final byte[] ID_VOR_6 = {0x20,0x06};
    public static final byte[] ID_VOR_7 = {0x20,0x07};
    public static final byte[] ID_VOR_8 = {0x20,0x08};
    public static final byte[] ID_VOR_9 = {0x20,0x09};
    public static final byte[] ID_VOR_10 = {0x20,0x10};
    public static final byte[] ID_VOR_11 = {0x20,0x11};
    public static final byte[] ID_VOR_12 = {0x20,0x12};
    public static final byte[] ID_VOR_13 = {0x20,0x13};

    //OXY
    public static final byte[] ID_OXY_A = {0x20,0x20};
    public static final byte[] ID_OXY_B = {0x20,0x21};


    /*
    呼吸机
     */

    //MI

}
