import os
import re

directory = 'src/test/java/hudson/plugins/emailext'

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            original_content = content
            
            content = content.replace('getFrom()String', 'fromString')
            content = content.replace('getFrom()Stdout', 'fromStdout')
            
            if content != original_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Fixed {filepath}')
