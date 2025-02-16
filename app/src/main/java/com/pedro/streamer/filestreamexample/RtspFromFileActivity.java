/*
 * Copyright (C) 2021 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.streamer.filestreamexample;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.pedro.encoder.input.decoder.AudioDecoderInterface;
import com.pedro.encoder.input.decoder.VideoDecoderInterface;
import com.pedro.library.rtsp.RtspFromFile;
import com.pedro.streamer.R;
import com.pedro.streamer.utils.PathUtils;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * More documentation see:
 * {@link com.pedro.library.base.FromFileBase}
 * {@link com.pedro.library.rtsp.RtspFromFile}
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RtspFromFileActivity extends AppCompatActivity
    implements ConnectCheckerRtsp, View.OnClickListener, VideoDecoderInterface,
    AudioDecoderInterface, SeekBar.OnSeekBarChangeListener {

  private RtspFromFile rtspFromFile;
  private Button button, bSelectFile, bReSync, bRecord;
  private SeekBar seekBar;
  private EditText etUrl;
  private TextView tvFile;
  private String filePath = "";
  private boolean touching = false;

  private String currentDateAndTime = "";
  private File folder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_from_file);
    folder = PathUtils.getRecordPath();
    button = findViewById(R.id.b_start_stop);
    bSelectFile = findViewById(R.id.b_select_file);
    button.setOnClickListener(this);
    bSelectFile.setOnClickListener(this);
    bReSync = findViewById(R.id.b_re_sync);
    bReSync.setOnClickListener(this);
    bRecord = findViewById(R.id.b_record);
    bRecord.setOnClickListener(this);
    etUrl = findViewById(R.id.et_rtp_url);
    etUrl.setHint(R.string.hint_rtsp);
    seekBar = findViewById(R.id.seek_bar);
    seekBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
    seekBar.setOnSeekBarChangeListener(this);
    tvFile = findViewById(R.id.tv_file);
    rtspFromFile = new RtspFromFile(this, this, this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (rtspFromFile.isRecording()) {
      rtspFromFile.stopRecord();
      PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
      bRecord.setText(R.string.start_record);
    }
    if (rtspFromFile.isStreaming()) {
      rtspFromFile.stopStream();
      button.setText(getResources().getString(R.string.start_button));
    }
  }

  @Override
  public void onConnectionStartedRtsp(@NotNull String rtspUrl) {
  }

  @Override
  public void onConnectionSuccessRtsp() {
    Toast.makeText(RtspFromFileActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onConnectionFailedRtsp(final String reason) {
    Toast.makeText(RtspFromFileActivity.this, "Connection failed. " + reason,
        Toast.LENGTH_SHORT).show();
    rtspFromFile.stopStream();
    button.setText(R.string.start_button);
  }

  @Override
  public void onNewBitrateRtsp(long bitrate) {

  }

  @Override
  public void onDisconnectRtsp() {
    Toast.makeText(RtspFromFileActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onAuthErrorRtsp() {
    Toast.makeText(RtspFromFileActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onAuthSuccessRtsp() {
    Toast.makeText(RtspFromFileActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 5 && data != null) {
      filePath = PathUtils.getPath(this, data.getData());
      Toast.makeText(this, filePath, Toast.LENGTH_SHORT).show();
      tvFile.setText(filePath);
    }
  }

  @Override
  public void onClick(View view) {
    int id = view.getId();
    if (id == R.id.b_start_stop) {
      if (!rtspFromFile.isStreaming()) {
        try {
          if (!rtspFromFile.isRecording()) {
            if (prepare()) {
              button.setText(R.string.stop_button);
              rtspFromFile.startStream(etUrl.getText().toString());
              seekBar.setMax(Math.max((int) rtspFromFile.getVideoDuration(),
                      (int) rtspFromFile.getAudioDuration()));
              updateProgress();
            } else {
              button.setText(R.string.start_button);
              rtspFromFile.stopStream();
                /*This error could be 2 things.
                 Your device cant decode or encode this file or
                 the file is not supported for the library.
                The file need has h264 video codec and acc audio codec*/
              Toast.makeText(this, "Error: unsupported file", Toast.LENGTH_SHORT).show();
            }
          } else {
            button.setText(R.string.stop_button);
            rtspFromFile.startStream(etUrl.getText().toString());
          }
        } catch (IOException e) {
          //Normally this error is for file not found or read permissions
          Toast.makeText(this, "Error: file not found", Toast.LENGTH_SHORT).show();
        }
      } else {
        button.setText(R.string.start_button);
        rtspFromFile.stopStream();
      }
    } else if (id == R.id.b_select_file) {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.setType("*/*");
      startActivityForResult(intent, 5);
      //sometimes async is produced when you move in file several times
    } else if (id == R.id.b_re_sync) {
      rtspFromFile.reSyncFile();
    } else if (id == R.id.b_record) {
      if (!rtspFromFile.isRecording()) {
        try {
          if (!folder.exists()) {
            folder.mkdir();
          }
          SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
          currentDateAndTime = sdf.format(new Date());
          if (!rtspFromFile.isStreaming()) {
            if (prepare()) {
              rtspFromFile.startRecord(
                      folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
              seekBar.setMax(Math.max((int) rtspFromFile.getVideoDuration(),
                      (int) rtspFromFile.getAudioDuration()));
              updateProgress();
              bRecord.setText(R.string.stop_record);
              Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(this, "Error preparing stream, This device cant do it",
                      Toast.LENGTH_SHORT).show();
            }
          } else {
            rtspFromFile.startRecord(
                    folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
            bRecord.setText(R.string.stop_record);
            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
          }
        } catch (IOException e) {
          rtspFromFile.stopRecord();
          PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
          bRecord.setText(R.string.start_record);
          Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
      } else {
        rtspFromFile.stopRecord();
        PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
        bRecord.setText(R.string.start_record);
        Toast.makeText(this,
                "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                Toast.LENGTH_SHORT).show();
        currentDateAndTime = "";
      }
    }
  }

  private boolean prepare() throws IOException {
    boolean result = rtspFromFile.prepareVideo(filePath);
    result |= rtspFromFile.prepareAudio(filePath);
    return result;
  }

  private void updateProgress() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while (rtspFromFile.isStreaming() || rtspFromFile.isRecording()) {
          try {
            Thread.sleep(1000);
            if (!touching) {
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  seekBar.setProgress(Math.max((int) rtspFromFile.getVideoTime(),
                      (int) rtspFromFile.getAudioTime()));
                }
              });
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  @Override
  public void onVideoDecoderFinished() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (rtspFromFile.isRecording()) {
          rtspFromFile.stopRecord();
          PathUtils.updateGallery(getApplicationContext(), folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
          bRecord.setText(R.string.start_record);
          Toast.makeText(RtspFromFileActivity.this,
              "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
              Toast.LENGTH_SHORT).show();
          currentDateAndTime = "";
        }
        if (rtspFromFile.isStreaming()) {
          button.setText(R.string.start_button);
          Toast.makeText(RtspFromFileActivity.this, "Video stream finished", Toast.LENGTH_SHORT)
              .show();
          rtspFromFile.stopStream();
        }
      }
    });
  }

  @Override
  public void onAudioDecoderFinished() {

  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    touching = true;
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    if (rtspFromFile.isStreaming() || rtspFromFile.isRecording()) {
      rtspFromFile.moveTo(seekBar.getProgress());
      //re sync after move to avoid async
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          rtspFromFile.reSyncFile();
        }
      }, 500);
    }
    touching = false;
  }
}
