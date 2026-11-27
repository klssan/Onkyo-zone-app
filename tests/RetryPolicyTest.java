package at.local.dualzone;
public class RetryPolicyTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  RetryPolicy r=new RetryPolicy();check(r.ready("wifi",100,false));r.failed(100);
  check(!r.ready("wifi",2099,false));check(r.ready("wifi",2100,false));r.failed(2100);
  check(!r.ready("wifi",6099,false));check(r.ready("mobile",2200,false));r.failed(2200);
  check(r.ready("mobile",2201,true));r.reset();check(r.ready("mobile",2202,false));
  for(int i=0;i<20;i++)r.failed(3000);check(!r.ready("mobile",62999,false));check(r.ready("mobile",63000,false));
  System.out.println("Retry backoff, network change, explicit retry and upper bound passed");
 }
}
