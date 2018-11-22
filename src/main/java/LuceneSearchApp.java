/*
 * Skeleton class for the Lucene search program implementation
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import sun.jvm.hotspot.oops.LongField;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LuceneSearchApp {
	
	public LuceneSearchApp() {

	}
	
	public void index(List<RssFeedDocument> docs) {
		// implement the Lucene indexing here
		try {
			Path iPath = Paths.get("./src/main/resources/bbc_rss_feed.xml.index");
			Directory directory = FSDirectory.open(iPath);
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
			indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			final IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

			docs.forEach(doc -> {
				Document d = new Document();

				Field titleField = new TextField("title", doc.getTitle(), Field.Store.YES);
				d.add(titleField);

				Field dateField = new LongPoint("date", doc.getPubDate().getTime());
				d.add(dateField);

				Field descriptionField = new TextField("description", doc.getDescription(), Field.Store.YES);
				d.add(descriptionField);

				try {
					indexWriter.addDocument( d );
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			indexWriter.commit();
			indexWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		docs.
//
//		File file = new File(path);
//		for (String filename : file.list()) {
//			indexWriter.addDocument( indexDoc(path+"/"+filename) );
//		}

		
	}
	
	public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
		
		printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

		List<String> results = new LinkedList<String>();

		Path path = Paths.get("./src/main/resources/bbc_rss_feed.xml.index");
		Directory directory = null;
		try {
			directory = FSDirectory.open(path);
			IndexReader indexReader = DirectoryReader.open(directory);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);

			BooleanQuery.Builder builder = new BooleanQuery.Builder();

			if(inTitle != null) {
				for (String title : inTitle) {
					builder = builder.add(
							new TermQuery(new Term("title", title)),
							BooleanClause.Occur.MUST
					);
				}
			}

			if(notInTitle != null) {
				for(String title : notInTitle) {
					builder = builder.add(
							new TermQuery(new Term("title", title)),
							BooleanClause.Occur.MUST_NOT
					);
				}
			}


			if(inDescription != null) {
				for(String description : inDescription) {
					builder = builder.add(
							new TermQuery(new Term("description", description)),
							BooleanClause.Occur.MUST
					);
				}
			}

			if(notInDescription != null) {
				for (String description : notInDescription) {
					builder = builder.add(
							new TermQuery(new Term("description", description)),
							BooleanClause.Occur.MUST_NOT
					);
				}
			}

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if(startDate != null && endDate != null) {
				try {
					long start = format.parse(startDate).getTime();
					long end = format.parse(endDate).getTime();
					builder = builder.add(
							LongPoint.newRangeQuery("date", start, end+86400000),
							BooleanClause.Occur.MUST
					);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if(endDate!= null) {
				try {
					long end = format.parse(endDate).getTime();
					builder = builder.add(
							LongPoint.newRangeQuery("date", 0, end+86400000),
							BooleanClause.Occur.MUST
					);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if(startDate != null) {
				try {
					long start = format.parse(startDate).getTime();
					builder = builder.add(
							LongPoint.newRangeQuery("date", start, Long.MAX_VALUE),
							BooleanClause.Occur.MUST
					);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

			BooleanQuery query = builder.build();

			TopDocs topDocs = indexSearcher.search(query, 30);
			ScoreDoc hits[] = topDocs.scoreDocs;


			for (ScoreDoc hit : hits) {
				results.add(indexSearcher.doc(hit.doc).getValues("title")[0]);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// implement the Lucene search here

		return results;
	}
	
	public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: "+inTitle);
			if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInTitle != null) {
			System.out.print("not in title: "+notInTitle);
			if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (inDescription != null) {
			System.out.print("in description: "+inDescription);
			if (notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInDescription != null) {
			System.out.print("not in description: "+notInDescription);
			if (startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (startDate != null) {
			System.out.print("startDate: "+startDate);
			if (endDate != null)
				System.out.print("; ");
		}
		if (endDate != null)
			System.out.print("endDate: "+endDate);
		System.out.println("):");
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) {
		args = new String[] { "./src/main/resources/bbc_rss_feed.xml" };

		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			RssFeedParser parser = new RssFeedParser();
			parser.parse(args[0]);
			List<RssFeedDocument> docs = parser.getDocuments();
			
			engine.index(docs);

			List<String> inTitle;
			List<String> notInTitle;
			List<String> inDescription;
			List<String> notInDescription;
			List<String> results;

			inTitle = new LinkedList<String>();
			inTitle.add("last");
			inTitle.add("us");
			results = engine.search(inTitle, null, null, null, null, null);
			engine.printResults(results);
			
			// 1) search documents with words "kim" and "korea" in the title
			inTitle = new LinkedList<String>();
			inTitle.add("kim");
			inTitle.add("korea");
			results = engine.search(inTitle, null, null, null, null, null);
			engine.printResults(results);
			
			// 2) search documents with word "kim" in the title and no word "korea" in the description
			inTitle = new LinkedList<String>();
			notInDescription = new LinkedList<String>();
			inTitle.add("kim");
			notInDescription.add("korea");
			results = engine.search(inTitle, null, null, notInDescription, null, null);
			engine.printResults(results);

			// 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
			inTitle = new LinkedList<String>();
			inTitle.add("us");
			notInTitle = new LinkedList<String>();
			notInTitle.add("dawn");
			inDescription = new LinkedList<String>();
			inDescription.add("american");
			inDescription.add("confession");
			results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
			engine.printResults(results);
			
			// 4) search documents whose publication date is 2011-12-18
			results = engine.search(null, null, null, null, "2011-12-19", "2011-12-19");
			engine.printResults(results);
			
			// 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
			inTitle = new LinkedList<String>();
			inTitle.add("video");
			results = engine.search(inTitle, null, null, null, "2000-01-01", null);
			engine.printResults(results);
			
			// 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
			notInDescription = new LinkedList<String>();
			notInDescription.add("canada");
			notInDescription.add("iraq");
			notInDescription.add("israel");
			results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
			engine.printResults(results);
		}
		else
			System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
	}
}
