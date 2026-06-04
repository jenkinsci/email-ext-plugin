import os
import re

directory = 'src/test/java/hudson/plugins/emailext'

replacements = {
    'defaultContent': ('getDefaultContent()', 'setDefaultContent'),
    'defaultSubject': ('getDefaultSubject()', 'setDefaultSubject'),
    'recipientList': ('getRecipientList()', 'setRecipientList'),
    'configuredTriggers': ('getConfiguredTriggers()', 'setConfiguredTriggers'),
    'attachmentsPattern': ('getAttachmentsPattern()', 'setAttachmentsPattern'),
}

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            original_content = content
            
            for field, (getter, setter) in replacements.items():
                # Replace assignment: publisher.fieldName = "value"; -> publisher.setFieldName("value");
                # This regex looks for: publisher.fieldName = <expression>;
                pattern_set = r'([a-zA-Z0-9_]+)\.' + field + r'\s*=\s*(.+?);'
                content = re.sub(pattern_set, lambda m: f"{m.group(1)}.{setter}({m.group(2)});", content)
                
                # Replace access: publisher.fieldName -> publisher.getFieldName()
                # We need to be careful not to replace it if it's already a method call (e.g., if there's a typo like fieldName() but that's unlikely here)
                # It should not match fieldName(
                pattern_get = r'([a-zA-Z0-9_]+)\.' + field + r'(?!\()'
                content = re.sub(pattern_get, lambda m: f"{m.group(1)}.{getter}", content)
            
            if content != original_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Fixed {filepath}')
