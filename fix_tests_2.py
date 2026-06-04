import os
import re

directory = 'src/test/java/hudson/plugins/emailext'

replacements = {
    'from': ('getFrom()', 'setFrom'),
    'contentType': ('getContentType()', 'setContentType'),
    'replyTo': ('getReplyTo()', 'setReplyTo'),
    'disabled': ('isDisabled()', 'setDisabled'),
    'attachBuildLog': ('isAttachBuildLog()', 'setAttachBuildLog'),
}

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            original_content = content
            
            for field, (getter, setter) in replacements.items():
                pattern_set = r'([a-zA-Z0-9_]+)\.' + field + r'\s*=\s*(.+?);'
                content = re.sub(pattern_set, lambda m: f"{m.group(1)}.{setter}({m.group(2)});", content)
                
                pattern_get = r'([a-zA-Z0-9_]+)\.' + field + r'(?!\()'
                content = re.sub(pattern_get, lambda m: f"{m.group(1)}.{getter}", content)
            
            if content != original_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Fixed {filepath}')
