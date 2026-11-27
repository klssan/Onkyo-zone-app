package at.local.dualzone;
/** Monotonic bounded retry schedule; a new physical network permits an immediate attempt. */
final class RetryPolicy {
 private long next;private int failures;private String link="";
 synchronized boolean ready(String key,long now,boolean explicit){
  if(!link.equals(key)){link=key;next=0;failures=0;}
  return explicit||now>=next;
 }
 synchronized void failed(long now){long delay=Math.min(60000L,2000L<<Math.min(failures,5));failures++;next=now+delay;}
 synchronized void reset(){next=0;failures=0;}
}
