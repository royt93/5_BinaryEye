from PIL import Image

def remove_white_bg(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    datas = img.getdata()
    
    newData = []
    for item in datas:
        # Check if the pixel is near white
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0)) # Transparent
        else:
            newData.append(item)
            
    img.putdata(newData)
    img.save(output_path, "PNG")

remove_white_bg("/Users/loitran/.gemini/antigravity/brain/7fa0c4a4-6b61-4876-98e4-c2b5080b03e6/crown_confetti_1777278646468.png", "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260318BinaryEye/app/src/main/res/drawable-nodpi/img_crown_confetti.png")
