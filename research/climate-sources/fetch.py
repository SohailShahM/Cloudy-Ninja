import urllib.request
import urllib.error
import urllib.parse
import xml.etree.ElementTree as ET
import os
import ssl
from datetime import datetime

# Ignore SSL errors for simplicity
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

out_dir = r"c:\Users\Radmin\Documents\Projects\cn-T-049\research\climate-sources"
urls_txt = os.path.join(out_dir, "urls.txt")
index_md = os.path.join(out_dir, "INDEX.md")

topics = [
    "water_cycle", "silver_iodide", "temperature_inversion", "albedo_effect", 
    "transpiration", "groundwater_recharge", "carbon_sequestration", 
    "storm_system", "biodiversity_index", "soil_microbiome", 
    "ocean_acidification", "cloud_seeding"
]

seed_urls = {
    "water_cycle": [
        "https://gpm.nasa.gov/education/water-cycle",
        "https://www.usgs.gov/special-topics/water-science-school/science/fundamentals-water-cycle",
        "https://oceanservice.noaa.gov/facts/watercycle.html"
    ],
    "silver_iodide": [
        "https://pubchem.ncbi.nlm.nih.gov/compound/Silver-iodide",
        "https://www.dri.edu/cloud-seeding-program/what-is-cloud-seeding/",
        "https://www.nj.gov/health/eoh/rtkweb/documents/fs/1672.pdf"
    ],
    "temperature_inversion": [
        "https://www.weather.gov/rnk/inversions",
        "https://www.weather.gov/lzk/inversion.htm",
        "https://deq.utah.gov/air-quality/inversions"
    ],
    "albedo_effect": [
        "https://climate.nasa.gov/explore/ask-nasa-climate/3266/how-albedo-affects-the-earths-climate/",
        "https://nsidc.org/cryosphere/seaice/processes/albedo.html",
        "https://earthobservatory.nasa.gov/images/84499/measuring-earths-albedo"
    ],
    "transpiration": [
        "https://www.usgs.gov/special-topics/water-science-school/science/evapotranspiration-and-water-cycle",
        "https://earthobservatory.nasa.gov/features/Water/page2.php",
        "https://water.usgs.gov/edu/watercycletranspiration.html"
    ],
    "groundwater_recharge": [
        "https://www.usgs.gov/special-topics/water-science-school/science/groundwater-decline-and-depletion",
        "https://pubs.usgs.gov/fs/1999/0103/report.pdf",
        "https://water.ca.gov/Programs/Groundwater-Management/SGMA-Groundwater-Management/Groundwater-Recharge"
    ],
    "carbon_sequestration": [
        "https://www.usgs.gov/faqs/what-carbon-sequestration",
        "https://www.fs.usda.gov/ccrc/topics/carbon-sequestration",
        "https://www.epa.gov/climate-research/carbon-sequestration-research"
    ],
    "storm_system": [
        "https://www.nssl.noaa.gov/education/svrwx101/thunderstorms/",
        "https://www.weather.gov/jetstream/tstorms_intro",
        "https://scied.ucar.edu/learning-zone/storms"
    ],
    "biodiversity_index": [
        "https://www.epa.gov/enviroatlas/biodiversity-metrics",
        "https://www.usgs.gov/centers/npwrc/science/biodiversity",
        "https://www.nature.com/scitable/knowledge/library/the-biodiversity-index-13228330"
    ],
    "soil_microbiome": [
        "https://www.pnnl.gov/science/soil-microbiome",
        "https://www.lbl.gov/research/soil-microbiome/",
        "https://science.osti.gov/-/media/ber/pdf/community-resources/Soil_Microbiome_Report.pdf"
    ],
    "ocean_acidification": [
        "https://www.noaa.gov/education/resource-collections/ocean-coasts/ocean-acidification",
        "https://oceanservice.noaa.gov/facts/acidification.html",
        "https://pmel.noaa.gov/co2/story/Ocean+Acidification"
    ],
    "cloud_seeding": [
        "https://www.dri.edu/cloud-seeding-program/",
        "https://www.weather.gov/media/fgf/presentations/fgf_cloudseeding.pdf",
        "https://cwcb.colorado.gov/focus-areas/supply/weather-modification-program"
    ]
}

results = []
valid_urls = []

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}

for topic in topics:
    urls = seed_urls.get(topic, [])
    success_count = 0
    
    for url in urls:
        if success_count >= 3:
            break
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, context=ctx, timeout=10) as response:
                if response.status == 200:
                    content_type = response.headers.get('Content-Type', '')
                    
                    authority = "gov" if ".gov" in url else ("edu" if ".edu" in url else "peer-review/nonprofit")
                    date = datetime.now().strftime("%Y-%m")
                    
                    if 'application/pdf' in content_type.lower() or url.endswith('.pdf'):
                        filename = f"{topic}_{success_count+1}.pdf"
                        filepath = os.path.join(out_dir, filename)
                        with open(filepath, 'wb') as f:
                            f.write(response.read())
                        results.append(f"| {filename} | {topic} | PDF | {authority} | {date} |")
                        print(f"Downloaded PDF for {topic}: {url}")
                    else:
                        valid_urls.append((topic, url))
                        results.append(f"| {url} | {topic} | URL | {authority} | {date} |")
                        print(f"Verified URL for {topic}: {url}")
                    success_count += 1
        except Exception as e:
            print(f"Failed {url}: {e}")
            
    # Fallback to arXiv
    if success_count < 3:
        needed = 3 - success_count
        query = topic.replace("_", " ")
        arxiv_url = f"http://export.arxiv.org/api/query?search_query=all:%22{urllib.parse.quote(query)}%22&start=0&max_results={needed}"
        try:
            with urllib.request.urlopen(arxiv_url, context=ctx, timeout=10) as response:
                xml_data = response.read()
                root = ET.fromstring(xml_data)
                for entry in root.findall("{http://www.w3.org/2005/Atom}entry"):
                    title = entry.find("{http://www.w3.org/2005/Atom}title").text.replace('\n', ' ')
                    pdf_link = entry.find("{http://www.w3.org/2005/Atom}link[@title='pdf']")
                    if pdf_link is not None:
                        pdf_url = pdf_link.attrib['href'] + ".pdf"
                        pub_date = entry.find("{http://www.w3.org/2005/Atom}published").text[:10]
                        
                        # Download PDF
                        req = urllib.request.Request(pdf_url, headers=headers)
                        with urllib.request.urlopen(req, context=ctx, timeout=15) as pdf_resp:
                            filename = f"{topic}_arxiv_{success_count+1}.pdf"
                            filepath = os.path.join(out_dir, filename)
                            with open(filepath, 'wb') as f:
                                f.write(pdf_resp.read())
                            results.append(f"| {filename} | {topic} | PDF | peer-review | {pub_date} |")
                            print(f"Downloaded arXiv PDF for {topic}: {pdf_url}")
                            success_count += 1
                    if success_count >= 3:
                        break
        except Exception as e:
            print(f"ArXiv fallback failed for {topic}: {e}")

with open(urls_txt, 'w', encoding='utf-8') as f:
    for t, u in valid_urls:
        f.write(f"{u} # {t}\n")

with open(index_md, 'w', encoding='utf-8') as f:
    f.write("# Climate Sources for NotebookLM\n\n")
    f.write("Curated, verified-live climate science sources for the Cloud Atlas expansion (T-045).\n\n")
    f.write("| Source (Filename/URL) | Topic | Type | Authority | Date |\n")
    f.write("|---|---|---|---|---|\n")
    for r in results:
        f.write(r + "\n")

print("Done generating sources!")
