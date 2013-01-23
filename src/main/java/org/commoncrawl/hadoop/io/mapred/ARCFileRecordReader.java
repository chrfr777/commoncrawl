package org.commoncrawl.hadoop.io.mapred;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.s3native.NativeS3FileSystem;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.InputSplit;
import org.commoncrawl.util.shared.ARCFileReader;
import org.jets3t.service.Jets3tProperties;

public class ARCFileRecordReader implements RecordReader<Text, BytesWritable> {

  protected Configuration conf;
  protected ARCFileReader reader;
  private long start;
  private long end;

  public void initialize(Configuration conf,InputSplit split)throws IOException {
    this.conf = conf;
    FileSplit fileSplit = (FileSplit) split;
    Path path = fileSplit.getPath();
    FileSystem fs = path.getFileSystem(conf);
    if (fs instanceof NativeS3FileSystem) { 
      Jets3tProperties properties = Jets3tProperties.getInstance(org.jets3t.service.Constants.JETS3T_PROPERTIES_FILENAME);
      properties.setProperty("s3service.https-only","false");
    }
    FSDataInputStream in = fs.open(path);
    reader = ARCFileReader.newReader(in,conf, path.toUri());
    start = fileSplit.getStart();
    end   = fileSplit.getLength();
    if (start != 0 || fs.getFileStatus(path).getLen() != end) { 
      throw new IOException("Invalid FileSplit encountered! Split Details:" + split.toString());
    }
  }

  
  @Override
  public boolean next(Text key, BytesWritable value) throws IOException {
    if (reader.hasMoreItems()) { 
      reader.nextKeyValue(key, value);
      return true;
    }
    return false;
  }

  @Override
  public Text createKey() {
    return new Text();
  }

  @Override
  public BytesWritable createValue() {
    return new BytesWritable();
  }

  @Override
  public long getPos() throws IOException {
    return reader.getPosition();
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @Override
  public float getProgress() throws IOException {
    return (float) reader.getPosition() / (float) end; 
  }
  
}
