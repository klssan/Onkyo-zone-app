package at.local.dualzone;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Arrays;

/** Device-bound encrypted private storage. Never export the configuration or log secrets. */
final class RemoteVault {
 static final String ALIAS="onkyo_remote_config_v1";
 static AtomicFile file(Context c){return new AtomicFile(new File(c.getNoBackupFilesDir(),"wireguard.enc"));}
 static boolean exists(Context c){return file(c).getBaseFile().isFile();}
 static SecretKey key(boolean create)throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
  if(!create)throw new IOException("Schlüssel fehlt");
  KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
  return kg.generateKey();
 }
 static void save(Context c,RemoteConfig cfg)throws Exception{
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(true));
  byte[] iv=cipher.getIV(),plain=cfg.encode(),encrypted;
  try{encrypted=cipher.doFinal(plain);}finally{Arrays.fill(plain,(byte)0);}
  AtomicFile af=file(c);FileOutputStream out=af.startWrite();
  try{out.write(iv.length);out.write(iv);out.write(encrypted);af.finishWrite(out);}catch(Exception e){af.failWrite(out);throw e;}
 }
 static RemoteConfig load(Context c)throws Exception{
  byte[] all=file(c).readFully();if(all.length<30||all[0]!=12||all.length>65536)throw new IOException("Konfiguration ungültig");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(false),new GCMParameterSpec(128,all,1,12));
  byte[] plain=cipher.doFinal(all,13,all.length-13);
  try{return RemoteConfig.decode(plain,c.getPackageName());}finally{Arrays.fill(plain,(byte)0);}
 }
 static void remove(Context c)throws Exception{
  file(c).delete();KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);ks.deleteEntry(ALIAS);
 }
}
