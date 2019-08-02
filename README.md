Create an IR system fetching documents corresponding to a query, rank the documents and display top 10 results.

Introduction: How we solved the task:
1. Get path of directories from user.
2. Fetch all files from the given folder and subfolders and populate an arrayList with all html
and txt files from the path where all were stored.
3. Create index for the fetched files at the path supplied by the user.
a. Add all the files in the list to Index them.
b. Create a document corresponding to each file and add it to the index.
c. Check if file is an html file or not, if it is an html file we are indexing the title also.
d. We have used Jsoup to parse html files.
e. Before adding files to index we used SimpleAnalyser to tokenize and Porter Stemmer
for stemming.
f. After pre-processing we added the documents to the index.
4. Searching the query in the index:
a. We processed the query string entered by user by stemming it.
b. In the next step a multi-field query is created where we passed fields as title and body,
i.e. we are searching the query concurrently over title (in case of an Html doc) and
body of the text.
c. We displayed top ten search results, with their Ranks, Relevance Scores, File names,
Titles (If an HTML document), Paths.
NOTE: File InformationRetrieval.Java contains the main source. All dependencies are also uploaded.
