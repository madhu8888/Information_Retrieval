
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.*;
import org.apache.lucene.index.*;

import org.jsoup.Jsoup;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class InformationRetrievalSystem {

	private static SimpleAnalyzer analyzer = new SimpleAnalyzer();
	private IndexWriter writer;
	// private ArrayList<File> queue = new ArrayList<File>();
	private static ArrayList<File> lstFiles = new ArrayList<File>();

	public static void main(String[] args) throws IOException {

		// 1. Path from where files needs to be extracted and at which index needs to be
		// created
		String DocumentFolder = args[0]; // F:\\OVGU\\Studies\\Information Retrieval\\Index_Created;
		String IndexFolder =args[1]; //"F:\\OVGU\\Studies\\Information Retrieval\\Index_Created";//  "";

		// 2. create an object of the class to call methods
		InformationRetrievalSystem InformationRetrievalObj = new InformationRetrievalSystem(IndexFolder);

		// 3. Fetch all files from the given folder and sub folders and populate the
		// list lstFiles
		File folder = new File(DocumentFolder);
		lstFiles = InformationRetrievalObj.FetchFiles(folder);

		// 4. Create index for the fetched files, first check whether the index exists
		// in the directory or not
		// if the index exists in the directory then don't create it else create it
		FSDirectory dir = FSDirectory.open(Paths.get(IndexFolder));
		if (!DirectoryReader.indexExists(dir)) {
			InformationRetrievalObj.IndexFileOrDirectory(IndexFolder);
		}
		// TODO:have to try VSM
	    //InformationRetrievalObj.VSMScoring(IndexFolder);
		InformationRetrievalObj.closeIndex();

		// 5. Searching the index for the query entered by user
		InformationRetrievalObj.SearchingQuery(IndexFolder,args[3]);

	}

	private void SearchingQuery(String IndexFolder, String query) throws IOException {
		// 5.1 Open the path of the index directory and create index if it does not
		// exist already
		FSDirectory dir = FSDirectory.open(Paths.get(IndexFolder));
		if (!DirectoryReader.indexExists(dir)) {
			IndexFileOrDirectory(IndexFolder);
			closeIndex();
		}

		// 5.2 create an Index IndexReaderObj object to read the index and an
		// IndexSearcher Object to search the index
		IndexReader IndexReaderObj = DirectoryReader.open(FSDirectory.open(Paths.get(IndexFolder)));
		IndexSearcher IndexSearcherObj = new IndexSearcher(IndexReaderObj);

		// 5.3 Collects the top 10 scoring doc for the given query
		TopScoreDocCollector collector = TopScoreDocCollector.create(10);

		String stemmedQuery = "";
		try {
			String queryEntered = query;

			// 5.4 Query stemmed before searching
			stemmedQuery = stemText(queryEntered);

			// 5.5 adding fields over which we want to query i.e c
			String[] fields = new String[2];
			fields[0] = "contents";
			fields[1] = "title";

			// 5.6 create a multifield query over contents and title
			Query queryObj = new MultiFieldQueryParser(fields, analyzer).parse(stemmedQuery);
			IndexSearcherObj.search(queryObj, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			// 5.7 print the Results with number of hits
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = IndexSearcherObj.doc(docId);
				if (d.get("path").toLowerCase().endsWith("html") || d.get("path").toLowerCase().endsWith("htm")) {
					File fileObj = new File(d.get("path"));
					org.jsoup.nodes.Document htmlDoc = Jsoup.parse(fileObj, "UTF-8", "");
					String title = htmlDoc.getElementsByTag("title").text();
					System.out.println("Rank:" + (i + 1) + " score:" + hits[i].score + " FileName:" + d.get("filename")
							+ " Title:" + title + " Path:" + d.get("path"));
				} else {
					System.out.println("Rank:" + (i + 1) + " score:" + hits[i].score + " FileName:" + d.get("filename")
							+ " Path:" + d.get("path"));
				}

			}

		} catch (Exception e) {
			System.out.println("Error searching " + stemmedQuery + " : " + e.getMessage());
		}
	}

	// Fetch all files to index from given path recursively
	private ArrayList<File> FetchFiles(File DocumentFolder) throws IOException {
       //3.1 Fetch all files from the given folder and populate an arrayList with all html and txt files 
		File[] listOfFiles = DocumentFolder.listFiles();
		for (File file : listOfFiles) {
			if (file.isDirectory()) {
				FetchFiles(file);
			} else {
				if (file.isFile() && (file.getName().endsWith(".txt") || file.getName().endsWith(".html")
						|| file.getName().endsWith(".htm"))) {
					lstFiles.add(file);
			  }
			}
		}

		return lstFiles;
	}

	// Constructor of the class , and initialize indexwriter
	InformationRetrievalSystem(String indexDir) throws IOException {

		FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		writer = new IndexWriter(dir, config);
	}

	private void IndexFileOrDirectory(String fileName) throws IOException {
       
		//4.1Add all the files in the list to Index to indx them
		for (File fileObj : lstFiles) {
			FileReader fileReaderObj = null;
			try {
				//4.2 Create a document corresponding to each file and add it to the index
				Document documentObj = new Document();
				fileReaderObj = new FileReader(fileObj);
				//4.3 Check if file is an html file or not if an html file add title also else make title as empty string
				if (fileObj.getName().endsWith("html") || fileObj.getName().endsWith("htm")) {
					
					//4.4 Using Jsoup to parse html files and do pre-processing
					org.jsoup.nodes.Document htmlDoc = Jsoup.parse(fileObj, "UTF-8", "");
					String title = htmlDoc.getElementsByTag("title").text();
					String body = htmlDoc.getElementsByTag("body").text();
					String stemTitle = stemText(title);
					String stemBody = stemText(body);
					documentObj.add(new TextField("contents", stemBody, Field.Store.YES));
					documentObj.add(new TextField("title", stemTitle, Field.Store.YES));
					documentObj.add(new StringField("path", fileObj.getPath(), Field.Store.YES));
					documentObj.add(new StringField("filename", fileObj.getName(), Field.Store.YES));

				} else {
					//4.5 Convert contents of a text file into string and do pre-preprocesing
					String fileText = readFile(fileObj);
					String stemData = stemText(fileText);
					documentObj.add(new TextField("contents", stemData, Field.Store.YES));
					documentObj.add(new TextField("title", "", Field.Store.YES));
					documentObj.add(new StringField("path", fileObj.getPath(), Field.Store.YES));
					documentObj.add(new StringField("filename", fileObj.getName(), Field.Store.YES));
				}

				writer.addDocument(documentObj);
			} catch (Exception e) {
				System.out.println("Error adding:"+ fileObj);
			} finally {
				fileReaderObj.close();
			}
		}
		lstFiles.clear();
	}

	private String readFile(File file) throws IOException {
		// Function to convert contents of a text file to a string
		byte[] encoded = null;
		String result;
		try {
			encoded = Files.readAllBytes(Paths.get(file.getPath()));

		} catch (Exception ex) {
			System.out.println(ex.getStackTrace());
		}
		result = new String(encoded, Charset.defaultCharset());
		return result;
	}

	private String stemText(String term) throws IOException {
		
		//4.4.1  A simple analyzer to tokenize 
		Analyzer analyzer = new SimpleAnalyzer();
		TokenStream result = analyzer.tokenStream(null, term);
		//4.4.2 Applying porter stemmer to find stem 
		result = new PorterStemFilter(result);
		CharTermAttribute resultAttr = result.addAttribute(CharTermAttribute.class);
		result.reset();

		StringBuffer str = new StringBuffer(" ");
		while (result.incrementToken()) {
			str.append(" " + resultAttr.toString());
		}

		result.close();
		analyzer.close();
		return str.toString();
	}

	/*private void addFiles(File file) {

		if (!file.exists()) {
			System.out.println(file + " does not exist.");
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				addFiles(f);
			}
		} else {
			String filename = file.getName().toLowerCase();

			if (filename.endsWith(".htm") || filename.endsWith(".html") || filename.endsWith(".xml")
					|| filename.endsWith(".txt")) {
				lstFiles.add(file);
			} else {
				System.out.println("Skipped " + filename);
			}
		}
	}*/

	private void closeIndex() throws IOException {
		// close the index
		writer.close();
	}

	/*public void parseHTMLUsingJsoup() {
		File input = new File("F:\\OVGU\\Studies\\Information Retrieval\\Docs_To_Index\\madhu.html");
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(input, "UTF-8", "");
			String title = doc.getElementsByTag("title").text();
			String body = doc.getElementsByTag("body").text();

			System.out.println("TITLE " + title);
			System.out.println("Body" + body);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/

	/*public void VSMScoring(String IndexFolder) throws IOException {

		int numDocs = writer.numDocs();
		FSDirectory dir = FSDirectory.open(Paths.get(IndexFolder));

		if (!DirectoryReader.indexExists(dir)) {
			IndexFileOrDirectory(IndexFolder);
			closeIndex();
		}
		IndexReader IndexReaderObj = DirectoryReader.open(FSDirectory.open(Paths.get(IndexFolder)));

		// try to find term frequencies of each document

		ClassicSimilarity simi = new ClassicSimilarity();

		Fields fields = MultiFields.getFields(IndexReaderObj);
		Iterator<String> fieldsIter = fields.iterator();
		while (fieldsIter.hasNext()) {
			String fieldname = fieldsIter.next();
			TermsEnum terms = fields.terms(fieldname).iterator();
			BytesRef term;
			while ((term = terms.next()) != null) {
				System.out.println(fieldname + ":" + term.utf8ToString() + " ttf:" + terms.totalTermFreq());
				// Or whatever else you want to do with it...
			}
		}*/
	//}

}
