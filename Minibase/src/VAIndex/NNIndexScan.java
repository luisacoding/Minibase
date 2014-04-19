package VAIndex;

import java.io.IOException;
import java.util.Arrays;

import index.IndexException;
import iterator.CondExpr;
import iterator.FldSpec;
import iterator.InvalidRelation;
import iterator.Iterator;
import iterator.JoinsException;
import iterator.PredEval;
import iterator.Projection;
import iterator.SortException;
import iterator.TupleUtils;
import iterator.TupleUtilsException;
import global.AttrType;
import global.IndexType;
import global.PageId;
import global.RID;
import global.Vector100Dtype;
import heap.FieldNumberOutOfBoundException;
import heap.Heapfile;
import heap.Scan;
import heap.Tuple;


public class NNIndexScan extends Iterator{
	private Vector100Dtype target;
	private Heapfile hf;// data file
	private VAFile vaf;
	private int _count;
	private int _b;// va bit
	private VACandidate vac[] = null;
	private int nextidx = 0;
	
	// copy fields
	private int  _fldNum;  // field number for indexed field
	private int  _noInFlds;
	private AttrType[]    _types;
	private short[]       _s_sizes; // input tuple string size
	private Tuple         Jtuple;// result tuple
	private CondExpr[]    _selects;
	public FldSpec[]      perm_mat;
	private int           _noOutFlds;
	private Tuple         tuplein;// input tuple
	private int           t1_size;
	
	/**
	 * define the nearest neighbor index scan
	 */
	public NNIndexScan(IndexType index,
			String heapfilename, String indName,
			AttrType[] types, short[] str_sizes, 
			int noInFlds, int noOutFlds, FldSpec[] outFlds,
			CondExpr[] selects,
			int fldNum,
			Vector100Dtype query, int count, int vabit)
					throws IndexException, VAException, 
					FieldNumberOutOfBoundException, IOException{
		//System.out.println("in VAFileNNScan");
		
		
		
		// ***************   copy1 begin  copy from IndexScan.java *************************//
	    _fldNum = fldNum;  
	    _noInFlds = noInFlds;
	    _types = types;  
	    _s_sizes = str_sizes;  
	    
	    
	    AttrType[] Jtypes = new AttrType[noOutFlds];
	    short[] ts_sizes;
	    Jtuple = new Tuple();
	    
	    
	    // setup  Jtuple  output tuple
	    try {
	        ts_sizes = TupleUtils.setup_op_tuple(Jtuple, Jtypes, types, noInFlds, str_sizes, outFlds, noOutFlds);
	      }
	      catch (TupleUtilsException e) {
	        throw new IndexException(e, "IndexScan.java: TupleUtilsException caught from TupleUtils.setup_op_tuple()");
	      }
	      catch (InvalidRelation e) {
	        throw new IndexException(e, "IndexScan.java: InvalidRelation caught from TupleUtils.setup_op_tuple()");
	      }
	    
	    _selects = selects;
	    perm_mat = outFlds;
	    _noOutFlds = noOutFlds;
	    
	    tuplein = new Tuple();    
	    try {
	    	tuplein.setHdr((short) noInFlds, types, str_sizes);
	    }
	    catch (Exception e) {
	      throw new IndexException(e, "IndexScan.java: Heapfile error");
	    }
	    
	    t1_size = tuplein.size();// input tuple size

	    
		// ***************   copy1 end  *************************//
	    
	    
	    // set again make sure it is correct
	    tuplein = new Tuple(t1_size);
	    try {
	    	tuplein.setHdr((short) noInFlds, types, str_sizes);
		    }
	    catch (Exception e) {
	      throw new IndexException(e, "IndexScan.java: Heapfile error");
	    }
	    
	    
		target = query;
		_b = vabit;
		_count = count;
		vac = new VACandidate[_count];
		
	    try {
	        hf = new Heapfile(heapfilename);
	      }
	      catch (Exception e) {
	        throw new IndexException(e, "VAFileNNScan.java: Heapfile not created");
	      }
	    
	    
	    try {
    		vaf = new VAFile(indName,-1); // we will set the bit later
	      }
	      catch (Exception e) {
		throw new IndexException(e, "VAFileNNScan.java: VAFile exceptions caught from VAFile constructor");
	      }
	    
	    VA_SSA();
	    nextidx = 0;
	    
	    
//	    for (int i=0;i<_count;i++){
//	    	   Tuple nexttuple = vac[1].getTuple();
////	    	   vac[i].getVector().printVector();
//	    	   
////	   		try{
////	   		Vector100Dtype tmpVec = nexttuple.get100DVectFld(1);
////	   		System.out.println("in NNScan debug");
////	   		tmpVec.printVector();//debug
////	   		
////	   		
////	   		}catch (Exception e) {
////
////	   		e.printStackTrace();
////	   		}
////	    	
//	    	   
//	    }
	}
	public Tuple get_next() throws IndexException {
//		System.out.println("in NN get next nextidx = "+ nextidx);
		if (nextidx == this._count)// no more 
			return null;
	   boolean eval;
	   RID nextrid = vac[nextidx].getRid();
	   Tuple nexttuple = null;//vac[nextidx].getTuple();
	   
		try{
//			System.out.println("get data pid="+nextrid.pageNo.pid);
			nexttuple = hf.getRecord(nextrid);
			tuplein.tupleCopy(nexttuple);
			
		}catch (Exception e) {

			e.printStackTrace();
		}
	   if (nexttuple == null)
		   return null;
	   tuplein.tupleCopy(nexttuple);

//	   System.out.println("in  NN get_next "+
//	   Arrays.toString(vac[nextidx].getTuple().returnTupleByteArray()));
	   

//		try{
//		Vector100Dtype tmpVec = nexttuple.get100DVectFld(1);
//		System.out.println("in VACandidate ");
//		tmpVec.printVector();//debug
//		
//		}catch (Exception e) {
//
//		e.printStackTrace();
//		}
	   
	   
	   
	   
	   nextidx++;
	   try {
		   TupleUtils.target = this.target;
		   eval = PredEval.Eval(_selects, tuplein, null, _types, null);
	   }
	   catch (Exception e) {
    	  throw new IndexException(e, "IndexScan.java: Heapfile error");
	   }
	      
      if (eval) {
		try {
//			System.out.println("call projection");//debug
		  Projection.Project(tuplein, _types, Jtuple, perm_mat, _noOutFlds);
		}
		catch (Exception e) {
		  throw new IndexException(e, "IndexScan.java: Heapfile error");
		}

    		return Jtuple;
	   }
      return null;
	}
	
	/**
	 * Simple Search algorithm
	 * 
	 * 
	 * @throws VAException
	 * @throws FieldNumberOutOfBoundException
	 * @throws IOException
	 */
	private void VA_SSA() throws VAException, FieldNumberOutOfBoundException, IOException{
		int largestdistance = initCandidate();
		RID rid1 = new RID();
		RID rid2 = new RID();
		Vector100Key vkey = null;
		KeyDataEntryVA keydata = null;
		Tuple temp = null;// for key
		Tuple temp2 = null;// for data
		Tuple t1 = new Tuple();
		// set tuple header for va key  t1
		AttrType[] attrType = new AttrType[1];
		attrType[0] = new AttrType(AttrType.attrVector100Dkey);
		short[] attrSize = new short[1];
		attrSize[0] = (short)(_b*100/8+8);		
		try {
			t1.setHdr((short) 1, attrType, attrSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int size = t1.size();
		t1 = new Tuple(size);
		try {
			t1.setHdr((short) 1, attrType, attrSize);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//open vafile index file
		Scan vascan = null;
		try {
			vascan =vaf.openScan();
		} catch (Exception e) {
			e.printStackTrace();
			Runtime.getRuntime().exit(1);
		}
		// get all keys
		Vector100Dtype tmpVec;
		try {
			temp = vascan.getNext(rid1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (temp != null) {
			//System.out.println("in VANN scan  fldcnt="+ temp.noOfFlds());
//			System.out.println("in VANN scan  temp.size="+ temp.size());
			//System.out.println("in VANN scan  fldcnt="+ t1.noOfFlds());
			t1.tupleCopy(temp);
			

			try {
//				System.out.println("in NNScan "+Arrays.toString(t1.returnTupleByteArray()).length());		
//				System.out.println("in NNScan "+Arrays.toString(t1.returnTupleByteArray())  );//debug
				
				keydata = t1.get100DVectKeyFld((short)1);
				vkey = keydata.getKey();
				rid2 = keydata.getRid();
					
//				System.out.println("in NNScan9 rid "+rid2.slotNo+" "+rid2.pageNo.pid);//debug
			} catch (Exception e) {

				e.printStackTrace();
			}
			vkey.setAllRegionNumber();
//			System.out.println("in VANN2 getLowerBoundDistance " 
//			+vkey.getLowerBoundDistance(this.target) );//debug
//			vkey.printAllRegionNumber();//debug

			if (vkey.getLowerBoundDistance(this.target) < largestdistance)
				// in this case, real data need to be fetched
				
				// if condition false, the key is filtered out
			{

				try{
					temp2 = hf.getRecord(rid2);
					tuplein.tupleCopy(temp2);
					
				}catch (Exception e) {

					e.printStackTrace();
				}
				tmpVec = tuplein.get100DVectFld(_fldNum);// get indexed field
				int realdistance = Vector100Dtype.distance(this.target, tmpVec);
//				System.out.println("in VANN2 realdistance "+realdistance);//debug
				largestdistance = this.Candidate(realdistance, rid2,tmpVec,tuplein);
			}
			
			//get next key
			try{

				temp = vascan.getNext(rid1);
				
			}catch (Exception e) {

				e.printStackTrace();
			}
		
		}
//		getResult();//debug
	}
	public VACandidate[] getResult() {
//		System.out.println(" in getResult "+ _count +" "+vac.length);//debug
		for (int i=0;i<_count;i++){
			System.out.println("in getResult2 "+vac[i].getDst());//debug
			vac[i].getVector().printVector();
			
		}
		return vac;
	}
	private int initCandidate(){
		int maxint = 0x7fffffff;// max int
		for (int i=0;i<this._count;i++){
			RID rid = new RID(new PageId(-1), -1);// invalid page, should be replaced later
			vac[i] = new VACandidate( maxint, rid);
			
		}
		return maxint;
	}
	private int Candidate (int realdst, RID rid, Vector100Dtype v, Tuple tuple){
//		System.out.println(" in Candidate**** "+realdst+" "+vac[0].getDst());//debug
//		System.out.println(" in Candidate**** "+realdst+" "+vac[1].getDst());
		if (realdst < vac[0].getDst())
		{
			vac[0] = new VACandidate(realdst, rid,v,tuple);
//			System.out.println("in Candidate size "+tuple.size());//debug
			sortCandidate();

		}
		return vac[0].getDst();
		
	}
	private void sortCandidate(){
		VACandidate tmpc = null;
		
		for (int i=0;i<this._count-1;i++){
			for (int j=i+1;j<this._count;j++){
				if (vac[i].getDst() < vac[j].getDst())
				{
					tmpc = vac[i];
					vac[i] = vac[j];
					vac[j] = tmpc;
				}
			}
		}
		
	}
	@Override
	public void close() throws IOException, JoinsException, SortException,
			IndexException
	{
	// TODO Auto-generated method stub
	
	}

}



class VACandidate {
	private int dst;
	private RID rid;
	private Vector100Dtype vector;
	private Tuple tuple;
	
	
	public Tuple getTuple() {
		return tuple;
	}
	public void setTuple(Tuple tuple) {
		this.tuple = tuple;
	}
	public RID getRid() {
		return rid;
	}
	public VACandidate(int dst, RID rid){
		this.dst = dst;
		this.rid = rid;
	}
	public Vector100Dtype getVector() {
		return vector;
	}
	public void setVector(Vector100Dtype vector) {
		this.vector = vector;
	}
	public VACandidate(int dst, RID rid, Vector100Dtype vector, Tuple tuple1) {
		this.dst = dst;
		this.rid = rid;
		this.vector = vector;
		this.tuple = new Tuple(tuple1.size());
		this.tuple.tupleCopy(tuple1);	
	}
	public int getDst() {
		return dst;
	}
}
