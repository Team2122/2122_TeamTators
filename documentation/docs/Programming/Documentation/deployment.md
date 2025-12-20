# Deployment

## CI (Continuous Integration)

CI is what runs in order to get the little green check mark in this image:

![Picture of a latest commit view on GitHub that has a green check mark](../../images/ci-checkmark.png)

What that green check mark means is that the documentation on that commit has
been successfully built and deployed to the `gh-pages` branch. This process is
defined in
[`.github/workflows/deploy-docs.yml`](https://github.com/Team2122/2122_TeamTators/blob/main/.github/workflows/deploy-docs.yml).
What that file does is define a job that calls `mkdocs gh-deploy` and tells
GitHub to run that job every time a commit is pushed the main branch. More info
on `mkdocs gh-deploy` can be found on the [MKDocs page](./mkdocs.md#gh-deploy)
or at the official [MKDocs
documentation](https://www.mkdocs.org/user-guide/deploying-your-docs/)

## GitHub Pages

The job of GitHub pages is to actually serve the documentation. GitHub pages is
a service that allows you to host static HTML sites for free at
`https://<user>.github.io/<repository-name>`.

For this repository, GitHub pages is configured to deploy the contents of the
`gh-pages` branch to this site anytime a commit is made to it. This enables the
following workflow for automatic docs deployment:

- Changes are made to main
- CI automatically runs `gh-deploy`, which builds the site and pushes the output
  to the `gh-pages`branch
- GitHub sees a change on the `gh-pages` branch and automatically pushes the
  output to the Tator Tome website
