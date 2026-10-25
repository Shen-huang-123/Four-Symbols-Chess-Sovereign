package com.dorm.fourchess;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small synthesized game sounds, independent of vendor system notification tones. */
final class GameSound {
    static final int SELECT=0, MOVE=1, CAPTURE=2, WIN=3;
    private final ExecutorService audio=Executors.newSingleThreadExecutor();
    private volatile boolean enabled=true;
    void setEnabled(boolean on){enabled=on;}
    void play(int effect){if(!enabled)return;audio.execute(()->render(effect));}
    private void render(int effect){
        int rate=22050;double[] notes;int noteMs;
        if(effect==SELECT){notes=new double[]{880};noteMs=65;}
        else if(effect==MOVE){notes=new double[]{520,680};noteMs=55;}
        else if(effect==CAPTURE){notes=new double[]{220,145};noteMs=90;}
        else {notes=new double[]{523.25,659.25,783.99,1046.5};noteMs=105;}
        int per=rate*noteMs/1000,total=per*notes.length;short[] pcm=new short[total];
        for(int n=0;n<notes.length;n++)for(int i=0;i<per;i++){
            double phase=2*Math.PI*notes[n]*i/rate;
            double attack=Math.min(1.0,i/(rate*.008));double release=Math.min(1.0,(per-i)/(rate*.025));
            double envelope=Math.max(0,Math.min(attack,release));
            pcm[n*per+i]=(short)(Math.sin(phase)*envelope*15000);
        }
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        AudioFormat format=new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        AudioTrack track=new AudioTrack(attrs,format,pcm.length*2,AudioTrack.MODE_STATIC,AudioManager.AUDIO_SESSION_ID_GENERATE);
        if(track.getState()!=AudioTrack.STATE_INITIALIZED){track.release();return;}
        track.write(pcm,0,pcm.length);track.setVolume(1f);track.play();
        try{Thread.sleep((long)noteMs*notes.length+40);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
        track.stop();track.release();
    }
    void release(){audio.shutdownNow();}
}
