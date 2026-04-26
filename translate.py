import sys
import xml.etree.ElementTree as ET
import urllib.request
import urllib.parse
import json
import os
import time

def batch_translate(texts, target_lang):
    if not texts: return []
    # mapping locales
    if target_lang == "iw": target_lang = "he"
    if target_lang == "in": target_lang = "id"
    
    # join texts with a separator that google translate won't destroy
    separator = " \n~|~|~\n "
    combined_text = separator.join(texts)
    
    url = f"https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl={target_lang}&dt=t"
    data = urllib.parse.urlencode({'q': combined_text}).encode('utf-8')
    try:
        req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
        response = urllib.request.urlopen(req, timeout=10)
        json_data = json.loads(response.read().decode('utf-8'))
        result = ''.join([sentence[0] for sentence in json_data[0] if sentence[0]])
        # split back
        translated_texts = [t.strip() for t in result.split("~ | ~ | ~")]
        if len(translated_texts) != len(texts):
            translated_texts = [t.strip() for t in result.split("~|~|~")]
        if len(translated_texts) != len(texts):
            translated_texts = [t.strip() for t in result.split("~ | ~ | ~")]
            
        # fallback if splitting fails
        if len(translated_texts) < len(texts):
            translated_texts += [""] * (len(texts) - len(translated_texts))
        return translated_texts[:len(texts)]
    except Exception as e:
        print(f"Error translating to {target_lang}: {e}")
        return texts

langs = ["ms", "my", "km", "lo", "ur", "ta", "te", "mr", "iw", "el", "ro", "sv", "fi", "ka", "hi", "th", "bn", "fa", "ar", "ko"]
source_file = "app/src/main/res/values/strings.xml"

tree = ET.parse(source_file)
root = tree.getroot()

# collect all texts
elements_to_translate = []
for elem in root.findall('string'):
    if elem.text and not elem.attrib.get('translatable') == 'false':
        elements_to_translate.append(elem)
        
for array_elem in root.findall('string-array'):
    for item in array_elem.findall('item'):
        if item.text and not item.text.startswith('@'):
            elements_to_translate.append(item)

texts_to_translate = [e.text for e in elements_to_translate]

for lang in langs:
    print(f"Translating for {lang}...")
    target_dir = f"app/src/main/res/values-{lang}"
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, "strings.xml")
    
    translated_texts = batch_translate(texts_to_translate, lang)
    
    new_tree = ET.parse(source_file)
    new_root = new_tree.getroot()
    
    idx = 0
    for elem in new_root.findall('string'):
        if elem.text and not elem.attrib.get('translatable') == 'false':
            t = translated_texts[idx]
            elem.text = t.replace("% s", "%s").replace("'", "\\'") if t else elem.text
            idx += 1
            
    for array_elem in new_root.findall('string-array'):
        for item in array_elem.findall('item'):
            if item.text and not item.text.startswith('@'):
                t = translated_texts[idx]
                item.text = t.replace("% s", "%s").replace("'", "\\'") if t else item.text
                idx += 1

    new_tree.write(target_file, encoding='utf-8', xml_declaration=True)
    time.sleep(1)

print("Translation done.")
