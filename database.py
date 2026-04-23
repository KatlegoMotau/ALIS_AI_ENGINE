from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# PostgreSQL connection URL
DATABASE_URL = "postgresql://postgres:Kaklegom@localhost:5432/alis_db"

# Create engine
engine = create_engine(
    DATABASE_URL,
    echo=True   # shows SQL in terminal (VERY useful for debugging)
)

# Create SessionLocal (this is what your main.py imports)
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

# Base class for models
Base = declarative_base()