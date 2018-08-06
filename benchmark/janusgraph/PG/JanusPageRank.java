package PG;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader; 

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.*;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;

import org.janusgraph.core.schema.*;
import org.janusgraph.util.datastructures.CompactMap;
import org.janusgraph.core.util.*;
import org.janusgraph.core.*;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import org.janusgraph.graphdb.database.management.*;

import java.io.FileWriter;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class JanusPageRank {
	public static JanusGraph JanusG;
	public static GraphTraversalSource ts;

	public static void main(String[] args){
		// args 0: property file
		String confPath = args[0];
		// args 1: vertexId file
		String rootFile = args[1];
                JanusG = JanusGraphFactory.open(confPath);
		try {
			// initialize output file 
			String resultFileName = "PageRank-latency-Graph500" ;
			String resultFilePath = "/4ebs/benchmark/code/janusgraph/result/" + resultFileName;
			FileWriter writer = new FileWriter(resultFilePath);
			writer.write("start pageRank, query time (in ms)\n");
			writer.flush();
			
			// set query timeout
			final Duration timeout = Duration.ofSeconds(86400);
			
			ExecutorService executor = Executors.newSingleThreadExecutor();

			// handle query timeout
			long startTime = System.nanoTime();
			final Future<String> handler = executor.submit(new Callable() {
				@Override
				 public String call() throws Exception {
					Future<ComputerResult> result = JanusG.compute().workers(4).program(PG.build().create(JanusG)).submit();
					Graph tmp = result.get().graph();

					// effective query time calculation                                              
                        		long duration = System.nanoTime() - startTime;                              
					writer.write("============================================\n"
						+ "total query time:\t" + duration/1000000.0 + "\n"
						+ "vertex,\tpgValue\n");

					writer.flush();
					System.out.println("######## total time #######  " + Double.toString((double)duration/1000000.0) + " ms");

					// print pagerank value for each vertex	
		                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rootFile)));
                		        final String line = reader.readLine();
                        		String[] vertex = line.split("\t");
	
					double maxdiff = -1; 
					for(String root : vertex){
						Object check = tmp.traversal().V().has("id", root).values("gremlin.pageRankVertexProgram.pageRank").next(); 
						double pgValue = (double) check;
						maxdiff = maxdiff < pgValue ? pgValue : maxdiff;
						writer.write("id: " + root + ",\t" + pgValue + "\n");
						writer.flush();
						System.out.println("id: " + root + ",\t" + pgValue + "\n");
					}
					writer.write("===================================================================\n" 
						+ "maxdiff: " + maxdiff + "\n");
					writer.flush();
					System.out.println("complete");
					return "complete"; 
				}
			});

			try {
				handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				handler.cancel(true);
				System.out.println("Exception occurred");
				e.printStackTrace();
			}
			
			executor.shutdownNow();			
			writer.close();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.exit(0);
	}


}


