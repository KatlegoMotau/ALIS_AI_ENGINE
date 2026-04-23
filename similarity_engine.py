# Import AI model for embeddings
from sentence_transformers import SentenceTransformer

# Import similarity calculation
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

# Load pre-trained NLP model
model = SentenceTransformer('all-MiniLM-L6-v2')


# Function to generate embedding from text
def generate_embedding(text):

    # Convert text into vector form
    return model.encode(text)

# Expose cosine similarity for other modules
def cosine_similarity_score(vec1, vec2):

    vec1 = np.array(vec1).reshape(1, -1)
    vec2 = np.array(vec2).reshape(1, -1)

    return cosine_similarity(vec1, vec2)[0][0]