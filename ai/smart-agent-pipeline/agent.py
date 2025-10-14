#!/usr/bin/env python3

import os
import sys
import json
import requests
from pathlib import Path
import git
import uuid
import subprocess
import urllib3
from urllib3.exceptions import InsecureRequestWarning

# Disable http warning about the missing SSL certificates to call OpenAI and GitHub APIs
urllib3.disable_warnings(InsecureRequestWarning)

# Collect all source code files from the project
def collect_source_files(directory):
    """Collect all important source code files from the project directory."""
    # Extended list of source file extensions
    source_extensions = {
        # Programming languages
        '.java', '.py', '.js', '.ts', '.jsx', '.tsx', '.go', '.rs', '.cpp', '.c', '.h', '.hpp',
        '.cs', '.rb', '.php', '.kt', '.scala', '.swift', '.dart', '.r', '.m', '.mm',
        # Web technologies
        '.html', '.css', '.scss', '.sass', '.less', '.vue', '.svelte',
        # Configuration and data files
        '.json', '.xml', '.yml', '.yaml', '.toml', '.ini', '.conf', '.config',
        '.properties', '.env', '.gitignore', '.dockerignore',
        # Build and dependency files
        '.gradle', '.pom', '.sbt', '.cargo', '.cabal', '.mix', '.gemfile', '.rakefile',
        # Database and SQL
        '.sql', '.ddl', '.dml', '.hql',
        # Documentation
        '.md', '.rst', '.txt', '.adoc',
        # Docker and deployment
        '.dockerfile', '.k8s.yml', '.k8s.yaml', '.yml', '.yaml'
    }
    
    # Directories to skip (dependency and build directories)
    skip_directories = {
        'node_modules', '__pycache__', '.git', '.svn', '.hg',
        'target', 'build', 'dist', 'out', 'bin', 'obj',
        '.gradle', '.maven', '.m2', '.sbt', '.ivy2',
        'vendor', 'deps', '_build', '.mix',
        'venv', 'env', '.venv', '.env', 'virtualenv',
        '.pytest_cache', '.coverage', '.nyc_output',
        'coverage', 'reports', 'logs', 'tmp', 'temp',
        '.idea', '.vscode', '.eclipse', '.netbeans',
        'bower_components', 'jspm_packages'
    }
    
    source_files = {}
    directory_path = Path(directory)
    
    if not directory_path.exists():
        print(f"ERROR: Directory not found: {directory}")
        return source_files

    for root, dirs, files in os.walk(directory_path):
        # Skip unwanted directories
        dirs[:] = [d for d in dirs if d not in skip_directories and not d.startswith('.')]
        
        for file in files:
            file_path = Path(root) / file
            relative_path = file_path.relative_to(directory_path)
            
            # Check if file should be included
            should_include = False
            
            # Check by extension
            if any(file.endswith(ext) for ext in source_extensions):
                should_include = True
            
            # Check by exact filename for special files
            special_files = {
                'Dockerfile', 'Makefile', 'CMakeLists.txt', 'configure.ac',
                'Vagrantfile', 'Jenkinsfile', 'Gruntfile.js', 'gulpfile.js',
                'webpack.config.js', 'rollup.config.js', 'vite.config.js',
                'tsconfig.json', 'jsconfig.json', 'babel.config.js',
                '.eslintrc', '.prettierrc', '.babelrc', 'jest.config.js',
                'pom.xml', 'build.gradle', 'package.json', 'requirements.txt',
                'Pipfile', 'Cargo.toml', 'go.mod', 'composer.json', 'Gemfile',
                'setup.py', 'pyproject.toml', 'docker-compose.yml', 'docker-compose.yaml'
            }
            if file in special_files:
                should_include = True
                
            # Skip if file is too large (>1MB)
            try:
                if file_path.stat().st_size > 1024 * 1024:
                    print(f"WARNING: Skipping large file: {relative_path}")
                    should_include = False
            except OSError:
                continue
                
            if should_include:
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        source_files[str(relative_path)] = content
                except (UnicodeDecodeError, OSError) as e:
                    print(f"WARNING: Could not read {relative_path}: {e}")
                    continue

    return source_files


# Create a comprehensive prompt for fixing build/test errors
def create_fix_prompt(source_files, build_error):
    """Create a prompt for OpenAI to analyze and fix the error."""
    source_files_section = ""
    for file_path, content in source_files.items():
        source_files_section += f"\n## File: {file_path}\n```\n{content}\n```\n"

    prompt = f"""You are an expert software developer and debugging specialist. I need help fixing build/test errors in my project.

## Project Context
This project contains source code that is failing to build or has failing tests. The project structure includes various source files, configuration files, and build scripts.

## Source Code Files
{source_files_section}

## Build/Test Error Output
```
{build_error}
```

## Request
Please analyze the build/test error and the provided source code, then:

1. **Identify the root cause** of the error
2. **Explain what's wrong** in detail
3. **Provide the exact fix** with complete corrected code files
4. **Explain why** this fix resolves the issue

Important guidelines:
- Only modify EXISTING files shown in the source code above
- Provide complete file contents for any files that need changes
- Do NOT create new files unless absolutely necessary
- Focus on fixing the specific error reported
- Maintain the exact file structure and formatting
- For test failures, analyze what the test expects vs what the code provides

Please provide your response in the following JSON format:
{{
  "summary_title": "Short summary of the fix with max of 30 characters",
  "analysis": "Detailed explanation of what's causing the error",
  "root_cause": "Brief summary of the root cause",
  "files_to_fix": [
    {{
      "file_path": "relative/path/to/file",
      "explanation": "Why this file needs to be changed",
      "corrected_content": "complete corrected file content here"
    }}
  ],
  "additional_notes": "Any additional information or warnings"
}}

Make sure the JSON is valid and the corrected_content contains the complete source file with the fix applied.
"""

    return prompt


# Send request to OpenAI to get build/test error fix
def get_error_fix(source_files, build_error):
    """Send request to OpenAI to get error fix."""
    
    api_token = os.environ.get('OPENAI_API_TOKEN')
    if not api_token:
        print("[AGENT] OPENAI_API_TOKEN not found in environment")
        sys.exit(1)

    prompt = create_fix_prompt(source_files, build_error)

    url = "https://api.openai.com/v1/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_token}"
    }

    data = {
        "model": "gpt-4o-mini",
        "messages": [
            {
                "role": "system",
                "content": "You are an expert software developer specializing in debugging build errors, test failures, and code issues across multiple programming languages and frameworks. You excel at analyzing error messages and providing precise fixes."
            },
            {
                "role": "user",
                "content": prompt
            }
        ],
        "max_tokens": 4000,
        "temperature": 0.1,
        "response_format": {"type": "json_object"}
    }

    try:
        response = requests.post(url, headers=headers, json=data, timeout=60, verify=False)
        response.raise_for_status()

        result = response.json()
        return result['choices'][0]['message']['content']

    except requests.exceptions.RequestException as e:
        print(f"[AGENT] Failed to call OpenAI API: {e}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"[AGENT] Failed to parse JSON response: {e}")
        sys.exit(1)


def apply_fixes(source_code_path, fix_response):
    """Apply the fixes suggested by OpenAI."""
    try:
        fix_data = json.loads(fix_response)
    except json.JSONDecodeError as e:
        print(f"[AGENT] Invalid JSON response: {e}")
        return False

    print(f"[AGENT] Analisys")
    print(fix_data.get('analysis', 'No analysis provided'))
    print(f"\n[AGENT] Root Cause")
    print(fix_data.get('root_cause', 'No root cause provided'))

    files_to_fix = fix_data.get('files_to_fix', [])
    if not files_to_fix:
        print("[AGENT] No files to fix identified")
        return False

    print(f"\n[AGENT] Applying fixes to {len(files_to_fix)} file(s)...")
    
    for fix in files_to_fix:
        file_path = fix.get('file_path')
        corrected_content = fix.get('corrected_content')
        explanation = fix.get('explanation', 'No explanation provided')
        
        if not file_path or not corrected_content:
            continue
        
        full_path = Path(source_code_path) / file_path
        print(f"\n[AGENT] Fixing file {full_path}")
        print(f"\n[AGENT] Fix Reason {explanation}")
        
        # Apply the fix
        try:
            # Ensure parent directory exists
            full_path.parent.mkdir(parents=True, exist_ok=True)
            
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(corrected_content)
            
        except Exception as e:
            print(f"[AGENT] Failed to fix {full_path}: {e}")
            return False
    
    # Show additional notes if any
    additional_notes = fix_data.get('additional_notes')
    if additional_notes:
        print()
        print(f"[AGENT] Additional notes {additional_notes}")
    
    return True


def open_github_pull_request(source_code_path, fix_response):  
    print("[AGENT] Opening pull request on GitHub...") 
    fix_data = json.loads(fix_response)

    github_token = os.environ.get('GITHUB_TOKEN')
    try:
        repo = git.Repo(source_code_path)
    except git.InvalidGitRepositoryError:
        print(f"[AGENT] '{source_code_path}' is not a valid Git repository.")
        sys.exit(1)

    origin = repo.remote(name="origin")
    original_url = origin.url
    origin.set_url(f"https://{github_token}@{original_url.split('://')[1]}")

    try:
        branch_name = f"fix/{uuid.uuid4()}"
        new_branch = repo.create_head(branch_name)
        new_branch.checkout()

        repo.git.add(A=True)  # Stage all changes

        # Commit the changes
        commit_message = "Apply automated fixes from OpenAI"
        repo.index.commit(commit_message)
        origin.push(new_branch.name)
    except git.GitCommandError as e:
        print(f"[AGENT] Error pushing changes: {e}")
        return False

    repo_name = original_url.split('.com/')[-1].replace('.git', '')

    url = f"https://api.github.com/repos/{repo_name}/pulls"
    headers = {
        "Authorization": f"token {github_token}",
        "Accept": "application/vnd.github.v3+json",
    }
    title = fix_data.get('summary_title', 'Automated Fixes from Jenkins Agent')
    body = format_pr_description(fix_data)
    data = {"title": title, "body": body, "head": new_branch.name, "base": "main"}
    response = requests.post(url, headers=headers, json=data, timeout=60, verify=False)

    if response.status_code == 201:
        pr_data = response.json()
        print(f"[AGENT] Pull request created {pr_data['html_url']}")
        return pr_data
    else:
        raise Exception(f"[AGENT] Failed to create PR httpStatus={response.status_code} - {response.text}")

    return True


def format_pr_description(fix_data):
    
    # Extract data from JSON with fallbacks
    summary_title = fix_data.get('summary_title', 'Automated Bug Fix')
    analysis = fix_data.get('analysis', 'No detailed analysis provided')
    root_cause = fix_data.get('root_cause', 'Root cause not identified')
    files_to_fix = fix_data.get('files_to_fix', [])
    additional_notes = fix_data.get('additional_notes', '')
    
    # Start building the PR description
    pr_description = f"""#  {summary_title}

##  Analysis

{analysis}

## Root Cause

{root_cause}

##  Files Modified

"""
    
    if files_to_fix:
        for i, file_fix in enumerate(files_to_fix, 1):
            file_path = file_fix.get('file_path', 'Unknown file')
            explanation = file_fix.get('explanation', 'No explanation provided')
            
            pr_description += f"""### {i}. `{file_path}`

**Why this file was changed:**
{explanation}

"""
    else:
        pr_description += "No files were modified.\n\n"
    
    # Add additional notes if present
    if additional_notes and additional_notes.strip():
        pr_description += f"""##  Additional Notes

{additional_notes}

"""
    
    pr_description += """
---
*This PR was automatically generated by AI-powered error fixing system* 
"""
    
    return pr_description


def main():
    print("\n" + "="*80)
    print("[AGENT] detected failure on build.")
    print("[AGENT] will analyze the error and suggest fixes.")

    if len(sys.argv) != 4:
        print("Usage: python agent.py <source_code_path> <build_error_file> <github_repo_token>")
        print("Example: python agent.py ./my-project /tmp/build_error.txt $github_repo_token")
        sys.exit(1)

    source_code_path = sys.argv[1]
    build_error_file = sys.argv[2]
    validation_command = sys.argv[3]

    # Read build error from file
    try:
        with open(build_error_file, 'r', encoding='utf-8') as f:
            build_error = f.read()
            print(f"[AGENT] collected build error: {build_error_file}")
    except FileNotFoundError:
        print(f"[AGENT] Did not found error file '{build_error_file}'")
        sys.exit(1)
    except Exception as e:
        print(f"[AGENT] Could not read build error file: {e}")
        sys.exit(1)

    print("[AGENT] Collecting source files...")
    source_files = collect_source_files(source_code_path)
    print(f"[AGENT] Found {len(source_files)} source files")

    if not source_files:
        print("[AGENT] No source files found in the specified directory")
        sys.exit(1)

    print("[AGENT] analysing error and getting fix suggestions...")
    fix_response = get_error_fix(source_files, build_error)
    
    if apply_fixes(source_code_path, fix_response):
        print("[AGENT] All fixes applied successfully!")
    else:
        print("[AGENT] Fixes could not be applied")
        sys.exit(1)

    print(f"\n[AGENT] Trying to validate the fixes using command '{validation_command}'...\n")
    result = subprocess.run(
        ["sh", "-c", f"cd {source_code_path} && {validation_command}"], 
        stdout=subprocess.DEVNULL, 
        stderr=subprocess.DEVNULL
    )
    if result.returncode == 0:
        print("[AGENT] Fixes were applied and validated successfully!")

        if open_github_pull_request(source_code_path, fix_response):
            print("[AGENT] Changes pushed to GitHub")
    else:
        print("[AGENT] Fixes were applied but the project build still failing. Sorry I could not help in this case :(")


if __name__ == "__main__":
    main()

