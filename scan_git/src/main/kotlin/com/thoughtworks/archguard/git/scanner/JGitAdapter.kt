package com.thoughtworks.archguard.git.scanner

import com.thoughtworks.archguard.git.scanner.complexity.CognitiveComplexityParser
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * @param path  repository location
 * @param branch  branch name, default is master
 */

class JGitAdapter(private val cognitiveComplexityParser: CognitiveComplexityParser) {

    fun scan(path: String, branch: String = "master", after: String = "0", repoId: String, systemId: Long): Pair<List<CommitLog>, List<ChangeEntry>> {
        val repPath = File(path)

        val repository = FileRepositoryBuilder().findGitDir(repPath).build()
        val git = Git(repository).specifyBranch(branch)
        val revCommitSequence = git.log().call().asSequence().takeWhile { it.commitTime * 1000L > after.toLong() }
        return (revCommitSequence.map { toCommitLog(it, repoId, systemId) }.toList() to
                revCommitSequence.map { toChangeEntry(repository, it) }.flatten().toList())
    }

    private fun toCommitLog(revCommit: RevCommit, repoId: String, systemId: Long): CommitLog {
        val committer = revCommit.committerIdent
        val msg = revCommit.shortMessage
        return CommitLog(id = revCommit.name,
                commitTime = committer.`when`.time,
                shortMessage = if (msg.length < 200) msg else msg.substring(0, 200),
                committerName = committer.name,
                committerEmail = committer.emailAddress,
                repositoryId = repoId, systemId = systemId)
    }

    private fun toChangeEntry(repository: Repository, revCommit: RevCommit): List<ChangeEntry> {
        val diffFormatter = DiffFormatter(DisabledOutputStream.INSTANCE).config(repository)
        return diffFormatter.scan(getParent(revCommit)?.tree, revCommit.tree)
                .map { d -> doCovertToChangeEntry(d, repository, revCommit) }
    }

    private fun getParent(revCommit: RevCommit): RevCommit? {
        return if (revCommit.parentCount == 0) {
            null
        } else
            revCommit.getParent(0)
    }

    private fun doCovertToChangeEntry(diffEntry: DiffEntry, repository: Repository, revCommit: RevCommit): ChangeEntry {
        val classComplexity: Int = cognitiveComplexityForJavaFile(diffEntry, repository, revCommit)
        return ChangeEntry(oldPath = diffEntry.oldPath,
                newPath = diffEntry.newPath,
                cognitiveComplexity = classComplexity,
                changeMode = diffEntry.changeType.name,
                commitId = revCommit.name)
    }

    private fun cognitiveComplexityForJavaFile(diffEntry: DiffEntry, repository: Repository, revCommit: RevCommit): Int {
        val treeWalk = TreeWalk.forPath(repository, diffEntry.newPath, revCommit.tree)
        if (treeWalk != null) {
            val objectId = treeWalk.getObjectId(0)
            val code = String(repository.newObjectReader().open(objectId).bytes, StandardCharsets.UTF_8)
            val cplx = cognitiveComplexityParser.processCode(code)
            return cplx.sumBy { it.complexity }
        }
        return 0
    }

    private fun DiffFormatter.config(repository: Repository): DiffFormatter {
        setRepository(repository)
        setDiffComparator(RawTextComparator.DEFAULT)
        isDetectRenames = true
        return this
    }

    /*specify git branch*/
    private fun Git.specifyBranch(branch: String): Git {
        checkout().setName(branch).call()
        return this
    }
}


