# Import libraries for reading documents
import pdfplumber     # Used to read PDF files
import docx           # Used to read DOCX files
import re             # Used for text splitting using patterns

# Function to extract text from a document
def extract_text(file_path):

    # Variable to store extracted text
    text = ""

    # If the file is a PDF
    if file_path.endswith(".pdf"):

         # Open the PDF file
        with pdfplumber.open(file_path) as pdf:

            # Loop through each page
            for page in pdf.pages:
                # Extract text from page
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"

    # If the file is a Word document
    elif file_path.endswith(".docx"):

        # Open the document
        document = docx.Document(file_path)

        # Loop through each paragraph
        for paragraph in document.paragraphs:
            # Add paragraph text to the main text variable
            text += paragraph.text + "\n"

    # Return all extracted text
    return text


# Function to split text into clauses
def segment_clauses(text):
    # Split text where clauses are numbered (1., 2., 3., etc.)
    clauses = re.split(r'\n\d+\.', text)
    
    # Return list of clauses
    return [c.strip() for c in clauses if c.strip()]