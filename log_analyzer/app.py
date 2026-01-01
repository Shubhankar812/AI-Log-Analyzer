import os
import boto3
from dotenv import load_dotenv

from langchain_core.documents import Document
from langchain_core.prompts import PromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import ChatOpenAI

# -------------------------------------------------
# 1️⃣ Load environment variables
# -------------------------------------------------
load_dotenv()

# -------------------------------------------------
# 2️⃣ S3 client
# -------------------------------------------------
s3 = boto3.client(
    "s3",
    aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID"),
    aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY"),
    region_name=os.getenv("AWS_DEFAULT_REGION")
)

BUCKET_NAME = "payment-simulator-logs-app-bucket"  
PREFIX = "logs/"   

# -------------------------------------------------
# 3️⃣ Read log files from S3
# -------------------------------------------------
log_texts = []

response = s3.list_objects_v2(
    Bucket=BUCKET_NAME,
    Prefix=PREFIX
)

for obj in response.get("Contents", []):
    key = obj["Key"]

    if not key.endswith(".log"):
        continue

    print(f"Reading {key}")

    s3_obj = s3.get_object(Bucket=BUCKET_NAME, Key=key)
    content = s3_obj["Body"].read().decode("utf-8")

    log_texts.append(content)

print(f"Loaded {len(log_texts)} log files")

# -------------------------------------------------
# 4️⃣ Convert to Documents
# -------------------------------------------------
documents = [
    Document(page_content=text)
    for text in log_texts
]

# -------------------------------------------------
# 5️⃣ Chunk logs
# -------------------------------------------------
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=1500,
    chunk_overlap=200
)

chunks = text_splitter.split_documents(documents)
print(f"Created {len(chunks)} chunks")

# -------------------------------------------------
# 6️⃣ Prompt
# -------------------------------------------------
analysis_prompt = PromptTemplate.from_template(
    """
You are a senior backend engineer analyzing production logs.

From the logs below, extract:
1. Errors and exceptions
2. Failed endpoints and HTTP status codes
3. Repeated or suspicious patterns
4. Performance issues (timeouts, slow responses)
5. Actionable recommendations

Logs:
{log_chunk}

Return a concise, structured analysis.
"""
)

# -------------------------------------------------
# 7️⃣ LCEL Chain (prompt | model | parser)
# -------------------------------------------------
model = ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0.2
)

parser = StrOutputParser()

analysis_chain = analysis_prompt | model | parser

# -------------------------------------------------
# 8️⃣ Run analysis on chunks (LIMIT FIRST)
# -------------------------------------------------
analyses = []

for i, chunk in enumerate(chunks[:5]):  # start with 5
    print(f"Analyzing chunk {i + 1}")
    result = analysis_chain.invoke(
        {"log_chunk": chunk.page_content}
    )
    analyses.append(result)

# -------------------------------------------------
# 9️⃣ Final aggregation (LCEL again)
# -------------------------------------------------
summary_prompt = PromptTemplate.from_template(
    """
You are an SRE lead.

Combine the following analyses into ONE final report with:
- Key issues
- Critical errors
- Impacted APIs
- Root-cause hints
- Clear next actions

Analyses:
{analyses}
"""
)

summary_chain = summary_prompt | model | parser

final_report = summary_chain.invoke(
    {"analyses": "\n\n".join(analyses)}
)

print("\n================ FINAL LOG ANALYSIS ================\n")
print(final_report)
